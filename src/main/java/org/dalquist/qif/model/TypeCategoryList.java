package org.dalquist.qif.model;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.LinkedListMultimap;

@HeaderLine(Header.HEADER_PREFIX + "Type:Cat")
public final class TypeCategoryList extends Header<TypeCategoryList.CategoryBlock> {
    private final LinkedHashMap<String, CategoryBlock> blocks;

    public TypeCategoryList() {
        blocks = new LinkedHashMap<>();
    }

    public TypeCategoryList(List<CategoryBlock> categoryBlocks) {
        this(categoryBlocks.stream());
    }

    public TypeCategoryList(Stream<CategoryBlock> categoryBlocks) {
        blocks = categoryBlocks.collect(
                Collectors.toMap(CategoryBlock::getName, Function.identity(), (x, y) -> y, LinkedHashMap::new));
    }

    // TODO add methods to interact with CategoryBlock data

    @Override
    public void parseBlock(LinkedListMultimap<Character, String> lines) {
        CategoryBlock categoryBlock = new CategoryBlock(lines);
        Preconditions.checkArgument(this.blocks.put(categoryBlock.name, categoryBlock) == null,
                "Duplicate category named: " + categoryBlock.name + "\n" + categoryBlock);
    }

    @Override
    public List<CategoryBlock> getBlocks() {
        return ImmutableList.copyOf(blocks.values());
    }

    public static final class CategoryBlock extends Block {
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

        /**
         * Expense category (if category is unspecified, Quicken assumes expense type)
         */
        @FieldPrefix("E")
        private Boolean expense;

        /** Budget amount (only in a Budget Amounts QIF file) */
        @FieldPrefix("B")
        private String budget;

        /** Income category */
        @FieldPrefix("I")
        private Boolean income;

        public CategoryBlock() {
        }

        CategoryBlock(LinkedListMultimap<Character, String> lines) {
            super(lines);

            if (income != null && income && expense != null && expense) {
                throw new IllegalStateException("Both income & expense are set!\n" + this);
            }
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

        public Boolean isTaxRelated() {
            return this.taxRelated;
        }

        public Boolean getTaxRelated() {
            return this.taxRelated;
        }

        public void setTaxRelated(Boolean taxRelated) {
            this.taxRelated = taxRelated;
        }

        public String getTaxSchedule() {
            return this.taxSchedule;
        }

        public void setTaxSchedule(String taxSchedule) {
            this.taxSchedule = taxSchedule;
        }

        public Boolean isExpense() {
            return this.expense;
        }

        public Boolean getExpense() {
            return this.expense;
        }

        public void setExpense(Boolean expense) {
            this.expense = expense;
        }

        public String getBudget() {
            return this.budget;
        }

        public void setBudget(String budget) {
            this.budget = budget;
        }

        public Boolean isIncome() {
            return this.income;
        }

        public Boolean getIncome() {
            return this.income;
        }

        public void setIncome(Boolean income) {
            this.income = income;
        }
    }
}
