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

package org.btc4j.core;

import java.io.File;
import java.util.List;

public interface BtcApi {
	public void addMultiSignatureAddress(int required, List<String> keys,
			String account) throws BtcException;

	public void addNode(String node, BtcNodeOperation operation)
			throws BtcException;

	public void backupWallet(File destination) throws BtcException;

	public String createMultiSignatureAddress(int required, List<String> keys)
			throws BtcException;

	public String createRawTransaction(List<Object> transactionIds,
			List<Object> addresses) throws BtcException;

	public String decodeRawTransaction(String transactionId)
			throws BtcException;

	public String dumpPrivateKey(String address) throws BtcException;

	public void encryptWallet(String passPhrase) throws BtcException;

	public String getAccount(String address) throws BtcException;

	public String getAccountAddress(String account) throws BtcException;

	public String getAddedNodeInformation(boolean dns, String node)
			throws BtcException;

	public List<String> getAddressesByAccount(String account)
			throws BtcException;

	public double getBalance(String account, int minConfirms)
			throws BtcException;

	public BtcBlock getBlock(String hash) throws BtcException;

	public int getBlockCount() throws BtcException;

	public String getBlockHash(int index) throws BtcException;

	public String getBlockTemplate(String params) throws BtcException;

	public int getConnectionCount() throws BtcException;

	public double getDifficulty() throws BtcException;

	public boolean getGenerate() throws BtcException;

	public int getHashesPerSecond() throws BtcException;

	public BtcStatus getInformation() throws BtcException;

	public BtcMining getMiningInformation() throws BtcException;

	public String getNewAddress(String account) throws BtcException;

	public List<BtcPeer> getPeerInformation() throws BtcException;

	public List<String> getRawMemoryPool() throws BtcException;

	public String getRawTransaction(String transactionId, boolean verbose)
			throws BtcException;

	public double getReceivedByAccount(String account, int minConfirms)
			throws BtcException;

	public double getReceivedByAddress(String address, int minConfirms)
			throws BtcException;

	public BtcTransaction getTransaction(String transactionId) throws BtcException;

	public String getTransactionOutput(String transactionId, int n,
			boolean includeMemoryPool) throws BtcException;

	public BtcTransactionOutputSet getTransactionOutputSetInformation()
			throws BtcException;

	public String getWork(String data) throws BtcException;

	public String help(String command) throws BtcException;

	public String importPrivateKey(String privateKey, String label,
			boolean rescan) throws BtcException;

	public void keyPoolRefill() throws BtcException;

	public List<BtcAccount> listAccounts(int minConfirms)
			throws BtcException;

	public List<BtcAddress> listAddressGroupings() throws BtcException;

	public List<String> listLockUnspent() throws BtcException;

	public List<BtcAccount> listReceivedByAccount(int minConfirms,
			boolean includeEmpty) throws BtcException;

	public List<BtcAddress> listReceivedByAddress(int minConfirms,
			boolean includeEmpty) throws BtcException;

	public BtcLastBlock listSinceBlock(String blockHash, int targetConfirms)
			throws BtcException;

	public List<BtcTransaction> listTransactions(String account, int count, int from)
			throws BtcException;

	public List<String> listUnspent(int minConfirms, int maxConfirms)
			throws BtcException;

	public void lockUnspent(boolean unlock, List<Object> outputs)
			throws BtcException;

	public void move(String fromAccount, String toAccount, double amount,
			int minConfirms, String comment) throws BtcException;

	public String sendFrom(String fromAccount, String toAddress, double amount,
			int minConfirms, String commentFrom, String commentTo)
			throws BtcException;

	public String sendMany(String fromAccount, List<Object> addresses,
			int minConfirms, String commentFrom, String commentTo)
			throws BtcException;

	public void sendRawTransaction(String transactionId)
			throws BtcException;

	public String sendToAddress(String toAddress, double amount,
			String commentFrom, String commentTo) throws BtcException;

	public void setAccount(String address, String account)
			throws BtcException;

	public void setGenerate(boolean generate, int generateProcessorsLimit)
			throws BtcException;

	public boolean setTransactionFee(double amount) throws BtcException;

	public void signMessage(String address, String message)
			throws BtcException;

	public void signRawTransaction(String transactionId,
			List<Object> signatures, List<String> keys) throws BtcException;

	public String stop() throws BtcException;

	public void submitBlock(String data, List<Object> params)
			throws BtcException;

	public BtcAddress validateAddress(String address)
			throws BtcException;

	public String verifyMessage(String address, String signature, String message)
			throws BtcException;
}