package org.dalquist.qif.model;

import java.util.List;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.LinkedListMultimap;

@HeaderLine(Header.HEADER_PREFIX + "Account")
public final class Account extends Header<Account.AccountBlock> {
    private AccountBlock block;

    public Account() {
    }

    // TODO add methods to interact with AccountBlock data

    @Override
    public void parseBlock(LinkedListMultimap<Character, String> lines) {
        if (block != null) {
            throw new IllegalStateException(this.getClass().getName() + " only supports a single block:\n" + lines);
        }
        this.block = new AccountBlock(lines);
    }

    @Override
    public List<AccountBlock> getBlocks() {
        if (block == null) {
            return ImmutableList.of();
        }
        return ImmutableList.of(block);
    }

    public AccountBlock getOrCreateBlock() {
        if (this.block == null) {
            this.block = new AccountBlock();
        }

        return this.block;
    }

    public void setBlock(AccountBlock block) {
        this.block = block;
    }

    public static final class AccountBlock extends Block {
        /** Name */
        @FieldPrefix("N")
        private String name;

        /** Description */
        @FieldPrefix("D")
        private String description;

        /** Type of account */
        @FieldPrefix("T")
        private String type;

        /** Credit limit (only for credit card account) */
        @FieldPrefix("L")
        private String limit;

        /** Statement balance date */
        @FieldPrefix("/")
        private String statementBalance;

        /** Statement balance */
        @FieldPrefix("$")
        private String statementBalanceDate;

        AccountBlock() {
        }

        AccountBlock(LinkedListMultimap<Character, String> lines) {
            super(lines);
        }

        public AccountBlock getBlock() {
            return this.block;
        }

        public String getName() {
            return this.name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getDescription() {
            return this.description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getType() {
            return this.type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getLimit() {
            return this.limit;
        }

        public void setLimit(String limit) {
            this.limit = limit;
        }

        public String getStatementBalance() {
            return this.statementBalance;
        }

        public void setStatementBalance(String statementBalance) {
            this.statementBalance = statementBalance;
        }

        public String getStatementBalanceDate() {
            return this.statementBalanceDate;
        }

        public void setStatementBalanceDate(String statementBalanceDate) {
            this.statementBalanceDate = statementBalanceDate;
        }
    }
}
