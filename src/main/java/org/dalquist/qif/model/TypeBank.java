
package org.dalquist.qif.model;

import java.util.ArrayList;
import java.util.List;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.LinkedListMultimap;

@HeaderLine(Header.HEADER_PREFIX + "Type:Bank")
public final class TypeBank extends Header {
    private final List<BankBlock> blocks = new ArrayList<>();

    public TypeBank() {
    }

    // TODO add methods to interact with BankBlock data

    @Override
    public void parseBlock(LinkedListMultimap<Character, String> lines) {
        this.blocks.add(new BankBlock(lines));
    }

    @Override
    public List<Block> getBlocks() {
        return ImmutableList.copyOf(blocks);
    }

    static final class BankBlock extends Block {
        /** Date */
        @FieldPrefix("D")
        private String date;

        /** Category (Category/Subcategory/Transfer/Class */
        @FieldPrefix("L")
        private String category;

        /** Memo */
        @FieldPrefix("M")
        private String memo;

        /** Amount */
        @FieldPrefix("T")
        private String ammount;

        /** Cleared status */
        @FieldPrefix("C")
        private String clearedStatus;

        /** Number(check or reference number) */
        @FieldPrefix("N")
        private String number;

        /** Payee */
        @FieldPrefix("P")
        private String payee;

        /** Address (up to five lines; the sixth line is an optional message) */
        @FieldPrefix("A")
        private String address;

        // TODO these need to be repeated ... how?
        // Maybe make them final List<> and then reflection does .add and constructor
        // verifies they are all the same length?

        @FieldPrefix(value = "S", blockType = SplitBlock.class)
        private List<SplitBlock> splitBlocks;

        BankBlock(LinkedListMultimap<Character, String> lines) {
            super(lines);
        }

        static final class SplitBlock extends Block {
            /** Category in split (Category/Transfer/Class) */
            @FieldPrefix("S")
            private String categoryInSplit;

            /** Memo in split */
            @FieldPrefix("E")
            private String memoInSplit;

            /** Dollar amount of split */
            @FieldPrefix("$")
            private String dollarAmmount;

            /** Percent. Optional—used if splits are done by percentage. */
            @FieldPrefix("%")
            private String percentAmmount;

            SplitBlock(LinkedListMultimap<Character, String> lines) {
                super(lines);
            }
        }
    }
}
