/*
 The MIT License (MIT)
 
 Copyright (c) 2013, 2014 by ggbusto@gmx.com

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

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class BtcBlock extends BtcBlockBase {
	private static final long serialVersionUID = -5115242454053420689L;
	private String hash = "";
	private long confirmations = 0;
	private String merkleRoot = "";
	private List<BtcTransaction> transactions = new ArrayList<BtcTransaction>();
	private long nonce = 0;
	private BigDecimal difficulty = BigDecimal.ZERO;
	private String nextBlockHash = "";

	public String getHash() {
		return hash;
	}

	public void setHash(String hash) {
		this.hash = BtcUtil.notNull(hash);
	}

	public long getConfirmations() {
		return confirmations;
	}

	public void setConfirmations(long confirmations) {
		this.confirmations = confirmations;
	}

	public String getMerkleRoot() {
		return merkleRoot;
	}

	public void setMerkleRoot(String merkleRoot) {
		this.merkleRoot = BtcUtil.notNull(merkleRoot);
	}

	public List<BtcTransaction> getTransactions() {
		return transactions;
	}

	public void setTransactions(List<BtcTransaction> transactions) {
		this.transactions = BtcUtil.notNull(transactions);
	}

	public long getNonce() {
		return nonce;
	}

	public void setNonce(long nonce) {
		this.nonce = nonce;
	}

	public BigDecimal getDifficulty() {
		return difficulty;
	}

	public void setDifficulty(BigDecimal difficulty) {
		this.difficulty = BtcUtil.notNull(difficulty);
	}

	public String getNextBlockHash() {
		return nextBlockHash;
	}

	public void setNextBlockHash(String nextBlockHash) {
		this.nextBlockHash = BtcUtil.notNull(nextBlockHash);
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("BtcBlock [hash=");
		builder.append(hash);
		builder.append(", confirmations=");
		builder.append(confirmations);
		builder.append(", size=");
		builder.append(getSize());
		builder.append(", height=");
		builder.append(getHeight());
		builder.append(", version=");
		builder.append(getVersion());
		builder.append(", merkleRoot=");
		builder.append(merkleRoot);
		builder.append(", transactions=");
		builder.append(transactions);
		builder.append(", time=");
		builder.append(getTime());
		builder.append(", nonce=");
		builder.append(nonce);
		builder.append(", bits=");
		builder.append(getBits());
		builder.append(", difficulty=");
		builder.append(difficulty);
		builder.append(", previousBlockHash=");
		builder.append(getPreviousBlockHash());
		builder.append(", nextBlockHash=");
		builder.append(nextBlockHash);
		builder.append("]");
		return builder.toString();
	}
}
