/*
 The MIT License (MIT)
 
 Copyright (c) 2013, 2014 by Guillermo Gonzalez, btc4j.org

 Permission is hereby granted, free of charge, to any person obtaining a copy
 of this software and associated documentation files (the "Software"), to deal
 in the Software without restriction, including without limitation the rights
 to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 copies of the Software, and to permit persons to whom the Software is
 furnished to do so, subject to the following conditions:

 The above copyright notice and this permission notice shall be included in all
 copies or substantial portions of the Software.

 THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 SOFTWARE.
 */

package org.btc4j.btc;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonNumber;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonString;
import javax.json.JsonValue;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpState;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.commons.httpclient.params.HttpClientParams;

public class BitcoinDaemon implements BitcoinApi {

	private final static Logger LOGGER = Logger.getLogger(BitcoinDaemon.class
			.getName());
	private final HttpState state;
	private final HttpClientParams params;
	private final URL url;

	private BitcoinDaemon(URL url, String account, String password,
			int timeoutInMillis) {
		this.url = url;
		state = new HttpState();
		state.setCredentials(new AuthScope(url.getHost(), url.getPort()),
				new UsernamePasswordCredentials(account, password));
		params = new HttpClientParams();
		params.setConnectionManagerTimeout(timeoutInMillis);
	}

	private static BitcoinDaemon makeDaemon(String host, int port,
			String account, String password, int timeoutInMillis,
			Process bitcoind) throws BitcoinException {
		URL url;
		try {
			url = new URL(BitcoinConstant.BTC4J_HTTP + "://" + host + ":"
					+ port);
		} catch (MalformedURLException e) {
			LOGGER.severe(String.valueOf(e));
			throw new BitcoinException(
					BitcoinConstant.BTC4J_ERROR_CODE,
					BitcoinConstant.BTC4J_ERROR_MESSAGE + ": " + e.getMessage(),
					e);
		}
		BitcoinDaemon daemon = new BitcoinDaemon(url, account, password,
				timeoutInMillis);
		int attempts = 0;
		boolean ping = false;
		String message = "";
		do {
			attempts++;
			try {
				LOGGER.info("attempt " + attempts + " of "
						+ BitcoinConstant.BTC4J_DAEMON_CONNECT_ATTEMPTS
						+ " to ping " + url);
				Thread.sleep(attempts * timeoutInMillis);
				BitcoinStatus info = daemon.getInformation();
				if (info != null) {
					ping = true;
					message = "connected bitcoind " + info.getVersion()
							+ " on " + url + " as " + account;
				}
			} catch (InterruptedException | BitcoinException e) {
				message = e.getMessage();
			}
		} while (!ping
				&& (attempts < BitcoinConstant.BTC4J_DAEMON_CONNECT_ATTEMPTS));
		if (!ping) {
			daemon = null;
			if (bitcoind != null) {
				bitcoind.destroy();
			}
			LOGGER.severe(message);
			throw new BitcoinException(BitcoinConstant.BTC4J_ERROR_CODE,
					BitcoinConstant.BTC4J_ERROR_MESSAGE + ": " + message);
		}
		LOGGER.info(message);
		return daemon;
	}

	public static BitcoinDaemon connectDaemon(String host, int port,
			final String account, final String password, int timeoutInMillis)
			throws BitcoinException {
		return makeDaemon(host, port, account, password, timeoutInMillis, null);
	}

	public static BitcoinDaemon runDaemon(File bitcoind, boolean testnet,
			String account, String password, int timeoutInMillis)
			throws BitcoinException {
		try {
			List<String> args = new ArrayList<String>();
			args.add(bitcoind.getCanonicalPath());
			if (testnet) {
				args.add(BitcoinConstant.BTC4J_DAEMON_ARG_TESTNET);
			}
			args.add(BitcoinConstant.BTC4J_DAEMON_ARG_ACCOUNT + account);
			args.add(BitcoinConstant.BTC4J_DAEMON_ARG_PASSWORD + password);
			LOGGER.info("args: " + args);
			return makeDaemon(BitcoinConstant.BTC4J_DAEMON_HOST,
					BitcoinConstant.BTC4J_DAEMON_PORT, account, password,
					timeoutInMillis, new ProcessBuilder(args).start());
		} catch (IOException e) {
			LOGGER.severe(String.valueOf(e));
			throw new BitcoinException(
					BitcoinConstant.BTC4J_ERROR_CODE,
					BitcoinConstant.BTC4J_ERROR_MESSAGE + ": " + e.getMessage(),
					e);
		}
	}

	public String[] getSupportedVersions() {
		return BitcoinConstant.BTC4J_DAEMON_VERSIONS;
	}

	protected JsonValue invoke(String method) throws BitcoinException {
		return invoke(method, null);
	}

	protected JsonValue invoke(String method, JsonValue parameters)
			throws BitcoinException {
		if (url == null) {
			LOGGER.severe(BitcoinConstant.BTC4J_ERROR_DATA_NULL_URL);
			throw new BitcoinException(BitcoinConstant.BTC4J_ERROR_CODE,
					BitcoinConstant.BTC4J_ERROR_MESSAGE + ": "
							+ BitcoinConstant.BTC4J_ERROR_DATA_NULL_URL);
		}
		PostMethod post = new PostMethod(url.toString());
		try {
			post.setRequestHeader(BitcoinConstant.BTC4J_HTTP_HEADER,
					BitcoinConstant.BTC4J_JSONRPC_CONTENT_TYPE);
			String guid = UUID.randomUUID().toString();
			JsonObjectBuilder builder = Json.createObjectBuilder();
			builder.add(BitcoinConstant.JSONRPC,
					BitcoinConstant.JSONRPC_VERSION).add(
					BitcoinConstant.JSONRPC_METHOD, method);
			if (parameters != null) {
				builder.add(BitcoinConstant.JSONRPC_PARAMS, parameters);
			} else {
				builder.addNull(BitcoinConstant.JSONRPC_PARAMS);
			}
			builder.add(BitcoinConstant.JSONRPC_ID, guid);
			JsonObject request = builder.build();
			LOGGER.info("request: " + request);
			post.setRequestEntity(new StringRequestEntity(request.toString(),
					BitcoinConstant.BTC4J_JSON_CONTENT_TYPE, null));
			HttpClient client = new HttpClient();
			client.setState(state);
			client.setParams(params);
			int status = client.executeMethod(post);
			if (status != HttpStatus.SC_OK) {
				LOGGER.severe(status + " " + HttpStatus.getStatusText(status));
				throw new BitcoinException(BitcoinConstant.BTC4J_ERROR_CODE,
						BitcoinConstant.BTC4J_ERROR_MESSAGE + ": " + status
								+ " " + HttpStatus.getStatusText(status));
			}
			JsonObject response = (JsonObject) Json.createReader(
					new StringReader(post.getResponseBodyAsString())).read();
			if (response == null) {
				LOGGER.severe(BitcoinConstant.BTC4J_ERROR_DATA_NULL_RESPONSE);
				throw new BitcoinException(
						BitcoinConstant.BTC4J_ERROR_CODE,
						BitcoinConstant.BTC4J_ERROR_MESSAGE
								+ ": "
								+ BitcoinConstant.BTC4J_ERROR_DATA_NULL_RESPONSE);
			}
			LOGGER.info("response: " + response);
			JsonString id = response.getJsonString(BitcoinConstant.JSONRPC_ID);
			if (id == null) {
				JsonObject error = response
						.getJsonObject(BitcoinConstant.JSONRPC_ERROR);
				if (error != null) {
					JsonObject data = error
							.getJsonObject(BitcoinConstant.JSONRPC_DATA);
					LOGGER.severe(String.valueOf(data));
					throw new BitcoinException(
							error.getInt(BitcoinConstant.JSONRPC_CODE),
							error.get(BitcoinConstant.JSONRPC_MESSAGE) + ": "
									+ data);
				} else {
					LOGGER.severe(BitcoinConstant.BTC4J_ERROR_DATA_INVALID_ERROR);
					throw new BitcoinException(
							BitcoinConstant.BTC4J_ERROR_CODE,
							BitcoinConstant.BTC4J_ERROR_MESSAGE
									+ ": "
									+ BitcoinConstant.BTC4J_ERROR_DATA_INVALID_ERROR);
				}
			}
			if (!guid.equals(id.getString())) {
				LOGGER.severe(BitcoinConstant.BTC4J_ERROR_DATA_INVALID_ID);
				throw new BitcoinException(BitcoinConstant.BTC4J_ERROR_CODE,
						BitcoinConstant.BTC4J_ERROR_MESSAGE + ": "
								+ BitcoinConstant.BTC4J_ERROR_DATA_INVALID_ID);
			}
			return response.get(BitcoinConstant.JSONRPC_RESULT);
		} catch (NullPointerException | ClassCastException | IOException e) {
			LOGGER.severe(String.valueOf(e));
			throw new BitcoinException(
					BitcoinConstant.BTC4J_ERROR_CODE,
					BitcoinConstant.BTC4J_ERROR_MESSAGE + ": " + e.getMessage(),
					e);
		} finally {
			post.releaseConnection();
		}
	}

	public void addMultiSignatureAddress(int required, List<String> keys)
			throws BitcoinException {
		addMultiSignatureAddress(required, keys, "");
	}

	@Override
	public void addMultiSignatureAddress(int required, List<String> keys,
			String account) throws BitcoinException {
		throw new BitcoinException(BitcoinConstant.BTC4J_ERROR_CODE,
				BitcoinConstant.BTC4J_ERROR_MESSAGE + ": "
						+ BitcoinConstant.BTC4J_ERROR_DATA_NOT_IMPLEMENTED);
	}

	@Override
	public void addNode(String node, BitcoinNodeOperationEnum operation)
			throws BitcoinException {
		throw new BitcoinException(BitcoinConstant.BTC4J_ERROR_CODE,
				BitcoinConstant.BTC4J_ERROR_MESSAGE + ": "
						+ BitcoinConstant.BTC4J_ERROR_DATA_NOT_IMPLEMENTED);
	}

	@Override
	public void backupWallet(File destination) throws BitcoinException {
		if (destination == null) {
			destination = new File(".");
		}
		JsonArray parameters = Json.createArrayBuilder()
				.add(destination.toString()).build();
		invoke(BitcoinConstant.BTCAPI_BACKUP_WALLET, parameters);
	}

	@Override
	public String createMultiSignatureAddress(int required, List<String> keys)
			throws BitcoinException {
		throw new BitcoinException(BitcoinConstant.BTC4J_ERROR_CODE,
				BitcoinConstant.BTC4J_ERROR_MESSAGE + ": "
						+ BitcoinConstant.BTC4J_ERROR_DATA_NOT_IMPLEMENTED);
	}

	@Override
	public String createRawTransaction(List<Object> transactionIds,
			List<Object> addresses) throws BitcoinException {
		throw new BitcoinException(BitcoinConstant.BTC4J_ERROR_CODE,
				BitcoinConstant.BTC4J_ERROR_MESSAGE + ": "
						+ BitcoinConstant.BTC4J_ERROR_DATA_NOT_IMPLEMENTED);
	}

	@Override
	public String decodeRawTransaction(String transactionId)
			throws BitcoinException {
		throw new BitcoinException(BitcoinConstant.BTC4J_ERROR_CODE,
				BitcoinConstant.BTC4J_ERROR_MESSAGE + ": "
						+ BitcoinConstant.BTC4J_ERROR_DATA_NOT_IMPLEMENTED);
	}

	@Override
	public String dumpPrivateKey(String address) throws BitcoinException {
		// TODO
		throw new BitcoinException(BitcoinConstant.BTC4J_ERROR_CODE,
				BitcoinConstant.BTC4J_ERROR_MESSAGE + ": "
						+ BitcoinConstant.BTC4J_ERROR_DATA_NOT_IMPLEMENTED);
	}

	@Override
	public void encryptWallet(String passPhrase) throws BitcoinException {
		// TODO
		throw new BitcoinException(BitcoinConstant.BTC4J_ERROR_CODE,
				BitcoinConstant.BTC4J_ERROR_MESSAGE + ": "
						+ BitcoinConstant.BTC4J_ERROR_DATA_NOT_IMPLEMENTED);
	}

	@Override
	public String getAccount(String address) throws BitcoinException {
		if (address == null) {
			address = "";
		}
		JsonArray parameters = Json.createArrayBuilder().add(address).build();
		JsonString resultss = (JsonString) invoke(
				BitcoinConstant.BTCAPI_GET_ACCOUNT, parameters);
		return resultss.getString();
	}

	@Override
	public String getAccountAddress(String account) throws BitcoinException {
		if (account == null) {
			account = "";
		}
		JsonArray parameters = Json.createArrayBuilder().add(account).build();
		JsonString results = (JsonString) invoke(
				BitcoinConstant.BTCAPI_GET_ACCOUNT_ADDRESS, parameters);
		return results.getString();
	}

	@Override
	public String getAddedNodeInformation(boolean dns, String node)
			throws BitcoinException {
		throw new BitcoinException(BitcoinConstant.BTC4J_ERROR_CODE,
				BitcoinConstant.BTC4J_ERROR_MESSAGE + ": "
						+ BitcoinConstant.BTC4J_ERROR_DATA_NOT_IMPLEMENTED);
	}

	@Override
	public List<String> getAddressesByAccount(String account)
			throws BitcoinException {
		if (account == null) {
			account = "";
		}
		JsonArray parameters = Json.createArrayBuilder().add(account).build();
		JsonArray results = (JsonArray) invoke(
				BitcoinConstant.BTCAPI_GET_ADDRESSES_BY_ACCOUNT, parameters);

		List<String> addresses = new ArrayList<String>();
		for (JsonString result : results.getValuesAs(JsonString.class)) {
			addresses.add(result.getString());
		}
		return addresses;
	}

	public double getBalance() throws BitcoinException {
		return getBalance("", 1);
	}

	public double getBalance(int minConfirms) throws BitcoinException {
		return getBalance("", minConfirms);
	}

	public double getBalance(String account) throws BitcoinException {
		return getBalance(account, 1);
	}

	@Override
	public double getBalance(String account, int minConfirms)
			throws BitcoinException {
		if (account == null) {
			account = "";
		}
		if (minConfirms < 1) {
			minConfirms = 1;
		}
		JsonArray parameters = Json.createArrayBuilder().add(account)
				.add(minConfirms).build();
		JsonNumber results = (JsonNumber) invoke(
				BitcoinConstant.BTCAPI_GET_BALANCE, parameters);
		return results.doubleValue();
	}

	@Override
	public BitcoinBlock getBlock(String hash) throws BitcoinException {
		if (hash == null) {
			hash = "";
		}
		JsonArray parameters = Json.createArrayBuilder().add(hash).build();
		JsonObject results = (JsonObject) invoke(
				BitcoinConstant.BTCAPI_GET_BLOCK, parameters);
		return BitcoinBlock.fromJson(results);
	}

	@Override
	public int getBlockCount() throws BitcoinException {
		JsonNumber results = (JsonNumber) invoke(BitcoinConstant.BTCAPI_GET_BLOCK_COUNT);
		return results.intValue();
	}

	@Override
	public String getBlockHash(int index) throws BitcoinException {
		if (index < 0) {
			index = 0;
		}
		JsonArray parameters = Json.createArrayBuilder().add(index).build();
		JsonString results = (JsonString) invoke(
				BitcoinConstant.BTCAPI_GET_BLOCK_HASH, parameters);
		return results.getString();
	}

	@Override
	public String getBlockTemplate(String params) throws BitcoinException {
		throw new BitcoinException(BitcoinConstant.BTC4J_ERROR_CODE,
				BitcoinConstant.BTC4J_ERROR_MESSAGE + ": "
						+ BitcoinConstant.BTC4J_ERROR_DATA_NOT_IMPLEMENTED);
	}

	@Override
	public int getConnectionCount() throws BitcoinException {
		JsonNumber results = (JsonNumber) invoke(BitcoinConstant.BTCAPI_GET_CONNECTION_COUNT);
		return results.intValue();
	}

	@Override
	public double getDifficulty() throws BitcoinException {
		JsonNumber results = (JsonNumber) invoke(BitcoinConstant.BTCAPI_GET_DIFFICULTY);
		return results.doubleValue();
	}

	@Override
	public boolean getGenerate() throws BitcoinException {
		JsonValue results = invoke(BitcoinConstant.BTCAPI_GET_GENERATE);
		return Boolean.valueOf(String.valueOf(results));
	}

	@Override
	public int getHashesPerSecond() throws BitcoinException {
		JsonNumber results = (JsonNumber) invoke(BitcoinConstant.BTCAPI_GET_HASHES_PER_SECOND);
		return results.intValue();
	}

	@Override
	public BitcoinStatus getInformation() throws BitcoinException {
		JsonObject results = (JsonObject) invoke(BitcoinConstant.BTCAPI_GET_INFORMATION);
		return BitcoinStatus.fromJson(results);
	}

	@Override
	public BitcoinMining getMiningInformation() throws BitcoinException {
		JsonObject results = (JsonObject) invoke(BitcoinConstant.BTCAPI_GET_MINING_INFORMATION);
		return BitcoinMining.fromJson(results);
	}

	public String getNewAddress() throws BitcoinException {
		return getNewAddress("");
	}

	@Override
	public String getNewAddress(String account) throws BitcoinException {
		JsonArray parameters = null;
		if ((account != null) && (account.length() > 0)) {
			parameters = Json.createArrayBuilder().add(account).build();
		}
		JsonString results = (JsonString) invoke(
				BitcoinConstant.BTCAPI_GET_NEW_ADDRESS, parameters);
		return results.getString();
	}

	@Override
	public List<BitcoinPeer> getPeerInformation() throws BitcoinException {
		JsonArray results = (JsonArray) invoke(BitcoinConstant.BTCAPI_GET_PEER_INFORMATION);
		List<BitcoinPeer> peers = new ArrayList<BitcoinPeer>();
		for (JsonObject result : results.getValuesAs(JsonObject.class)) {
			peers.add(BitcoinPeer.fromJson(result));
		}
		return peers;
	}

	@Override
	public List<String> getRawMemoryPool() throws BitcoinException {
		JsonArray results = (JsonArray) invoke(BitcoinConstant.BTCAPI_GET_RAW_MEMORY_POOL);
		List<String> rawMemPool = new ArrayList<String>();
		for (JsonString result : results.getValuesAs(JsonString.class)) {
			rawMemPool.add(result.getString());
		}
		return rawMemPool;
	}

	@Override
	public String getRawTransaction(String transactionId, boolean verbose)
			throws BitcoinException {
		// TODO
		throw new BitcoinException(BitcoinConstant.BTC4J_ERROR_CODE,
				BitcoinConstant.BTC4J_ERROR_MESSAGE + ": "
						+ BitcoinConstant.BTC4J_ERROR_DATA_NOT_IMPLEMENTED);
	}

	public double getReceivedByAccount(String account) throws BitcoinException {
		return getReceivedByAccount(account, 1);
	}

	@Override
	public double getReceivedByAccount(String account, int minConfirms)
			throws BitcoinException {
		if (account == null) {
			account = "";
		}
		if (minConfirms < 1) {
			minConfirms = 1;
		}
		JsonArray parameters = Json.createArrayBuilder().add(account)
				.add(minConfirms).build();
		JsonNumber results = (JsonNumber) invoke(
				BitcoinConstant.BTCAPI_GET_RECEIVED_BY_ACCOUNT, parameters);
		return results.doubleValue();
	}

	public double getReceivedByAddress(String address) throws BitcoinException {
		return getReceivedByAddress(address, 1);
	}

	@Override
	public double getReceivedByAddress(String address, int minConfirms)
			throws BitcoinException {
		if (address == null) {
			address = "";
		}
		if (minConfirms < 1) {
			minConfirms = 1;
		}
		JsonArray parameters = Json.createArrayBuilder().add(address)
				.add(minConfirms).build();
		JsonNumber results = (JsonNumber) invoke(
				BitcoinConstant.BTCAPI_GET_RECEIVED_BY_ADDRESS, parameters);
		return results.doubleValue();
	}

	@Override
	public String getTransaction(String transactionId) throws BitcoinException {
		// TODO
		if (transactionId == null) {
			transactionId = "";
		}
		JsonArray parameters = Json.createArrayBuilder().add(transactionId)
				.build();
		JsonValue results = invoke(BitcoinConstant.BTCAPI_GET_TRANSACTION,
				parameters);
		return String.valueOf(results);
	}

	@Override
	public String getTransactionOutput(String transactionId, int n,
			boolean includeMemoryPool) throws BitcoinException {
		throw new BitcoinException(BitcoinConstant.BTC4J_ERROR_CODE,
				BitcoinConstant.BTC4J_ERROR_MESSAGE + ": "
						+ BitcoinConstant.BTC4J_ERROR_DATA_NOT_IMPLEMENTED);
	}

	@Override
	public BitcoinTransactionOutputSet getTransactionOutputSetInformation()
			throws BitcoinException {
		JsonObject results = (JsonObject) invoke(BitcoinConstant.BTCAPI_GET_TRANSACTION_OUTPUT_SET_INFORMATION);
		return BitcoinTransactionOutputSet.fromJson(results);
	}

	@Override
	public String getWork(String data) throws BitcoinException {
		throw new BitcoinException(BitcoinConstant.BTC4J_ERROR_CODE,
				BitcoinConstant.BTC4J_ERROR_MESSAGE + ": "
						+ BitcoinConstant.BTC4J_ERROR_DATA_NOT_IMPLEMENTED);
	}

	public String help() throws BitcoinException {
		return help("");
	}

	@Override
	public String help(String command) throws BitcoinException {
		JsonArray parameters = null;
		if ((command != null) && (command.length() > 0)) {
			parameters = Json.createArrayBuilder().add(command).build();
		}
		JsonString results = (JsonString) invoke(BitcoinConstant.BTCAPI_HELP,
				parameters);
		return results.getString();
	}

	@Override
	public String importPrivateKey(String privateKey, String label,
			boolean reScan) throws BitcoinException {
		throw new BitcoinException(BitcoinConstant.BTC4J_ERROR_CODE,
				BitcoinConstant.BTC4J_ERROR_MESSAGE + ": "
						+ BitcoinConstant.BTC4J_ERROR_DATA_NOT_IMPLEMENTED);
	}

	@Override
	public void keyPoolRefill() throws BitcoinException {
		throw new BitcoinException(BitcoinConstant.BTC4J_ERROR_CODE,
				BitcoinConstant.BTC4J_ERROR_MESSAGE + ": "
						+ BitcoinConstant.BTC4J_ERROR_DATA_NOT_IMPLEMENTED);
	}

	public Map<String, BitcoinAccount> listAccounts() throws BitcoinException {
		return listAccounts(1);
	}

	@Override
	public Map<String, BitcoinAccount> listAccounts(int minConfirms)
			throws BitcoinException {
		if (minConfirms < 1) {
			minConfirms = 1;
		}
		JsonArray parameters = Json.createArrayBuilder().add(minConfirms)
				.build();
		JsonObject results = (JsonObject) invoke(
				BitcoinConstant.BTCAPI_LIST_ACCOUNTS, parameters);
		Map<String, BitcoinAccount> accounts = new HashMap<String, BitcoinAccount>();
		for (String account : results.keySet()) {
			JsonNumber amount = results.getJsonNumber(account);
			accounts.put(account,
					new BitcoinAccount(account, amount.doubleValue(), 0));
		}
		return accounts;
	}

	@Override
	public List<String> listAddressGroupings() throws BitcoinException {
		JsonArray results = (JsonArray) invoke(BitcoinConstant.BTCAPI_LIST_ADDRESS_GROUPINGS);
		List<String> groupings = new ArrayList<String>();
		for (JsonObject grouping : results.getValuesAs(JsonObject.class)) {
			groupings.add(String.valueOf(grouping));
		}
		return groupings;
	}

	@Override
	public List<String> listLockUnspent() throws BitcoinException {
		JsonArray results = (JsonArray) invoke(BitcoinConstant.BTCAPI_LIST_LOCK_UNSPENT);
		List<String> unspents = new ArrayList<String>();
		for (JsonObject unspent : results.getValuesAs(JsonObject.class)) {
			unspents.add(String.valueOf(unspent));
		}
		return unspents;
	}

	public List<BitcoinAccount> listReceivedByAccount() throws BitcoinException {
		return listReceivedByAccount(1, false);
	}
	
	@Override
	public List<BitcoinAccount> listReceivedByAccount(int minConfirms,
			boolean includeEmpty) throws BitcoinException {
		if (minConfirms < 0) {
			minConfirms = 1;
		}
		JsonArray parameters = Json.createArrayBuilder().add(minConfirms).add(includeEmpty)
				.build();
		JsonArray results = (JsonArray) invoke(BitcoinConstant.BTCAPI_LIST_RECEIVED_BY_ACCOUNT, parameters);
		List<BitcoinAccount> accounts = new ArrayList<BitcoinAccount>();
		for (JsonObject result : results.getValuesAs(JsonObject.class)) {
			accounts.add(BitcoinAccount.fromJson(result));
		}
		return accounts;
	}

	public List<BitcoinAddress> listReceivedByAddress() throws BitcoinException {
		return listReceivedByAddress(1, false);
	}
	
	@Override
	public List<BitcoinAddress> listReceivedByAddress(int minConfirms,
			boolean includeEmpty) throws BitcoinException {
		if (minConfirms < 0) {
			minConfirms = 1;
		}
		JsonArray parameters = Json.createArrayBuilder().add(minConfirms).add(includeEmpty)
				.build();
		JsonArray results = (JsonArray) invoke(BitcoinConstant.BTCAPI_LIST_RECEIVED_BY_ADDRESS, parameters);
		List<BitcoinAddress> addresses = new ArrayList<BitcoinAddress>();
		for (JsonObject result : results.getValuesAs(JsonObject.class)) {
			addresses.add(BitcoinAddress.fromJson(result));
		}
		return addresses;
	}

	public List<String> listSinceBlock() throws BitcoinException {
		return listSinceBlock("", 1);
	}
	
	@Override
	public List<String> listSinceBlock(String blockHash, int targetConfirms)
			throws BitcoinException {
		// TODO
		// listsinceblock [blockhash] [target-confirmations]
		// Get all transactions in blocks since block [blockhash], or all transactions if omitted
		throw new BitcoinException(BitcoinConstant.BTC4J_ERROR_CODE,
				BitcoinConstant.BTC4J_ERROR_MESSAGE + ": "
						+ BitcoinConstant.BTC4J_ERROR_DATA_NOT_IMPLEMENTED);
	}

	public List<String> listTransactions() throws BitcoinException {
		return listTransactions("", 10, 0);
	}
	
	public List<String> listTransactions(String account) throws BitcoinException {
		return listTransactions(account, 10, 0);
	}
	
	public List<String> listTransactions(String account, int count) throws BitcoinException {
		return listTransactions(account, count, 0);
	}
			
	@Override
	public List<String> listTransactions(String account, int count, int from)
			throws BitcoinException {
		if (account == null) {
			account = "";
		}
		if (count < 1) {
			count = 10;
		}
		if (from < 0) {
			from = 0;
		}
		JsonArray parameters = Json.createArrayBuilder().add(account).add(count).add(from)
				.build();
		JsonArray results = (JsonArray) invoke(BitcoinConstant.BTCAPI_LIST_TRANSACTIONS, parameters);
		List<String> transactions = new ArrayList<String>();
		for (JsonObject transaction : results.getValuesAs(JsonObject.class)) {
			transactions.add(String.valueOf(transaction));
		}
		return transactions;
	}

	@Override
	public List<String> listUnspent(int minConfirms, int maxConfirms)
			throws BitcoinException {
		throw new BitcoinException(BitcoinConstant.BTC4J_ERROR_CODE,
				BitcoinConstant.BTC4J_ERROR_MESSAGE + ": "
						+ BitcoinConstant.BTC4J_ERROR_DATA_NOT_IMPLEMENTED);
	}

	@Override
	public void lockUnspent(boolean unlock, List<Object> outputs)
			throws BitcoinException {
		throw new BitcoinException(BitcoinConstant.BTC4J_ERROR_CODE,
				BitcoinConstant.BTC4J_ERROR_MESSAGE + ": "
						+ BitcoinConstant.BTC4J_ERROR_DATA_NOT_IMPLEMENTED);
	}

	@Override
	public void move(String fromAccount, String toAccount, double amount,
			int minConfirms, String comment) throws BitcoinException {
		throw new BitcoinException(BitcoinConstant.BTC4J_ERROR_CODE,
				BitcoinConstant.BTC4J_ERROR_MESSAGE + ": "
						+ BitcoinConstant.BTC4J_ERROR_DATA_NOT_IMPLEMENTED);
	}

	@Override
	public String sendFrom(String fromAccount, String toAddress, double amount,
			int minConfirms, String commentFrom, String commentTo)
			throws BitcoinException {
		throw new BitcoinException(BitcoinConstant.BTC4J_ERROR_CODE,
				BitcoinConstant.BTC4J_ERROR_MESSAGE + ": "
						+ BitcoinConstant.BTC4J_ERROR_DATA_NOT_IMPLEMENTED);
	}

	@Override
	public String sendMany(String fromAccount, List<Object> addresses,
			int minConfirms, String commentFrom, String commentTo)
			throws BitcoinException {
		throw new BitcoinException(BitcoinConstant.BTC4J_ERROR_CODE,
				BitcoinConstant.BTC4J_ERROR_MESSAGE + ": "
						+ BitcoinConstant.BTC4J_ERROR_DATA_NOT_IMPLEMENTED);
	}

	@Override
	public void sendRawTransaction(String transactionId)
			throws BitcoinException {
		throw new BitcoinException(BitcoinConstant.BTC4J_ERROR_CODE,
				BitcoinConstant.BTC4J_ERROR_MESSAGE + ": "
						+ BitcoinConstant.BTC4J_ERROR_DATA_NOT_IMPLEMENTED);
	}

	@Override
	public String sendToAddress(String toAddress, double amount,
			String commentFrom, String commentTo) throws BitcoinException {
		throw new BitcoinException(BitcoinConstant.BTC4J_ERROR_CODE,
				BitcoinConstant.BTC4J_ERROR_MESSAGE + ": "
						+ BitcoinConstant.BTC4J_ERROR_DATA_NOT_IMPLEMENTED);
	}

	@Override
	public void setAccount(String address, String account)
			throws BitcoinException {
		if (address == null) {
			address = "";
		}
		if (account == null) {
			account = "";
		}
		JsonArray parameters = Json.createArrayBuilder().add(address)
				.add(account).build();
		invoke(BitcoinConstant.BTCAPI_SET_ACCOUNT, parameters);
	}

	@Override
	public void setGenerate(boolean generate, int generateProcessorsLimit)
			throws BitcoinException {
		if (generateProcessorsLimit < 1) {
			generateProcessorsLimit = -1;
		}
		JsonArray parameters = Json.createArrayBuilder().add(generate)
				.add(generateProcessorsLimit).build();
		invoke(BitcoinConstant.BTCAPI_SET_GENERATE, parameters);
	}

	public void setGenerate(boolean generate) throws BitcoinException {
		setGenerate(generate, -1);
	}

	@Override
	public boolean setTransactionFee(double amount) throws BitcoinException {
		if (amount < 0) {
			amount = 0;
		}
		JsonArray parameters = Json.createArrayBuilder().add(amount).build();
		JsonValue results = invoke(BitcoinConstant.BTCAPI_SET_TRANSACTION_FEE, parameters);
		return Boolean.valueOf(String.valueOf(results));
	}

	@Override
	public void signMessage(String address, String message)
			throws BitcoinException {
		throw new BitcoinException(BitcoinConstant.BTC4J_ERROR_CODE,
				BitcoinConstant.BTC4J_ERROR_MESSAGE + ": "
						+ BitcoinConstant.BTC4J_ERROR_DATA_NOT_IMPLEMENTED);
	}

	@Override
	public void signRawTransaction(String transactionId,
			List<Object> signatures, List<String> keys) throws BitcoinException {
		throw new BitcoinException(BitcoinConstant.BTC4J_ERROR_CODE,
				BitcoinConstant.BTC4J_ERROR_MESSAGE + ": "
						+ BitcoinConstant.BTC4J_ERROR_DATA_NOT_IMPLEMENTED);
	}

	@Override
	public String stop() throws BitcoinException {
		JsonString results = (JsonString) invoke(BitcoinConstant.BTCAPI_STOP);
		return results.getString();
	}

	@Override
	public void submitBlock(String data, List<Object> params)
			throws BitcoinException {
		throw new BitcoinException(BitcoinConstant.BTC4J_ERROR_CODE,
				BitcoinConstant.BTC4J_ERROR_MESSAGE + ": "
						+ BitcoinConstant.BTC4J_ERROR_DATA_NOT_IMPLEMENTED);
	}

	@Override
	public BitcoinAddress validateAddress(String address)
			throws BitcoinException {
		if (address == null) {
			address = "";
		}
		JsonArray parameters = Json.createArrayBuilder().add(address).build();
		JsonObject results = (JsonObject) invoke(
				BitcoinConstant.BTCAPI_VALIDATE_ADDRESS, parameters);
		return BitcoinAddress.fromJson(results);
	}

	@Override
	public String verifyMessage(String address, String signature, String message)
			throws BitcoinException {
		throw new BitcoinException(BitcoinConstant.BTC4J_ERROR_CODE,
				BitcoinConstant.BTC4J_ERROR_MESSAGE + ": "
						+ BitcoinConstant.BTC4J_ERROR_DATA_NOT_IMPLEMENTED);
	}
}