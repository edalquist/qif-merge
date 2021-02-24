package org.dalquist.qif.model;

import java.util.List;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.LinkedListMultimap;

@HeaderLine(Header.HEADER_PREFIX + "Account")
public final class Account extends Header {
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
    public List<Block> getBlocks() {
        if (block == null) {
            return ImmutableList.of();
        }
        return ImmutableList.of(block);
    }

    final class AccountBlock extends Block {
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

        AccountBlock(LinkedListMultimap<Character, String> lines) {
            super(lines);
        }
    }
}
