
package org.dalquist.qif.model;

import java.util.ArrayList;
import java.util.List;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.LinkedListMultimap;

@HeaderLine(Header.HEADER_PREFIX + "Type:Bank")
public final class TypeBank extends Header<TypeBank.BankBlock> {
    private final List<BankBlock> blocks = new ArrayList<>();

    public TypeBank() {
    }

    // TODO add methods to interact with BankBlock data

    @Override
    public void parseBlock(LinkedListMultimap<Character, String> lines) {
        this.blocks.add(new BankBlock(lines));
    }

    @Override
    public List<BankBlock> getBlocks() {
        return ImmutableList.copyOf(blocks);
    }

    public static final class BankBlock extends Block {
        /** Date */
        @FieldPrefix(value = "D", printWhenEmpty = true)
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

            if (splitBlocks == null) {
                splitBlocks = new ArrayList<>();
            }
        }

        public String getDate() {
            return this.date;
        }

        public void setDate(String date) {
            this.date = date;
        }

        public String getCategory() {
            return this.category;
        }

        public void setCategory(String category) {
            this.category = category;
        }

        public String getMemo() {
            return this.memo;
        }

        public void setMemo(String memo) {
            this.memo = memo;
        }

        public String getAmmount() {
            return this.ammount;
        }

        public void setAmmount(String ammount) {
            this.ammount = ammount;
        }

        public String getClearedStatus() {
            return this.clearedStatus;
        }

        public void setClearedStatus(String clearedStatus) {
            this.clearedStatus = clearedStatus;
        }

        public String getNumber() {
            return this.number;
        }

        public void setNumber(String number) {
            this.number = number;
        }

        public String getPayee() {
            return this.payee;
        }

        public void setPayee(String payee) {
            this.payee = payee;
        }

        public String getAddress() {
            return this.address;
        }

        public void setAddress(String address) {
            this.address = address;
        }

        public List<SplitBlock> getSplitBlocks() {
            return splitBlocks;
        }

        public static final class SplitBlock extends Block {
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

            public String getCategoryInSplit() {
                return this.categoryInSplit;
            }

            public void setCategoryInSplit(String categoryInSplit) {
                this.categoryInSplit = categoryInSplit;
            }

            public String getMemoInSplit() {
                return this.memoInSplit;
            }

            public void setMemoInSplit(String memoInSplit) {
                this.memoInSplit = memoInSplit;
            }

            public String getDollarAmmount() {
                return this.dollarAmmount;
            }

            public void setDollarAmmount(String dollarAmmount) {
                this.dollarAmmount = dollarAmmount;
            }

            public String getPercentAmmount() {
                return this.percentAmmount;
            }

            public void setPercentAmmount(String percentAmmount) {
                this.percentAmmount = percentAmmount;
            }

            @Override
            protected String getBlockEndline() {
                return "";
            }
        }
    }
}
