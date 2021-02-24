package org.dalquist.qif.model;

import java.util.LinkedHashMap;
import java.util.List;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.LinkedListMultimap;

@HeaderLine(Header.HEADER_PREFIX + "Type:Cat")
public final class TypeCategoryList extends Header {
    private final LinkedHashMap<String, CategoryBlock> blocks = new LinkedHashMap<>();

    public TypeCategoryList() {
    }

    // TODO add methods to interact with CategoryBlock data

    @Override
    public void parseBlock(LinkedListMultimap<Character, String> lines) {
        CategoryBlock categoryBlock = new CategoryBlock(lines);
        Preconditions.checkArgument(this.blocks.put(categoryBlock.name, categoryBlock) == null,
                "Duplicate category named: " + categoryBlock.name + "\n" + categoryBlock);
    }

    @Override
    public List<Block> getBlocks() {
        return ImmutableList.copyOf(blocks.values());
    }

    final class CategoryBlock extends Block {
        /** Category name:subcategory name */
        @FieldPrefix("N")
        private String name;

        /** Description */
        @FieldPrefix("D")
        private String description;

        /** Tax related if included, not tax related if omitted */
        @FieldPrefix("T")
        private Boolean taxRelated;

        /** Tax schedule information */
        @FieldPrefix("R")
        private String taxSchedule;

        /** Income category */
        @FieldPrefix("E")
        private Boolean expense;

        /** Budget amount (only in a Budget Amounts QIF file) */
        @FieldPrefix("B")
        private String budget;

        /**
         * Expense category (if category is unspecified, Quicken assumes expense type)
         */
        @FieldPrefix("I")
        private Boolean income;

        CategoryBlock(LinkedListMultimap<Character, String> lines) {
            super(lines);

            if (income != null && income && expense != null && expense) {
                throw new IllegalStateException("Both income & expense are set!\n" + this);
            }
        }
    }
}
