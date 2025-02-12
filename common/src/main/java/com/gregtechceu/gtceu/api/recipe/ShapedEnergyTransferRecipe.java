package com.gregtechceu.gtceu.api.recipe;

import com.google.gson.JsonObject;
import com.gregtechceu.gtceu.api.capability.GTCapabilityHelper;
import com.gregtechceu.gtceu.api.capability.IElectricItem;
import com.gregtechceu.gtceu.api.registry.GTRegistries;
import com.gregtechceu.gtceu.core.mixins.ShapedRecipeInvoker;
import lombok.Getter;
import net.minecraft.FieldsAreNonnullByDefault;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.Level;

import java.util.Map;

/**
 * @author Irgendwer01
 * @date 2023/11/4
 * @implNote ShapedEnergyTransferRecipe
 */
@MethodsReturnNonnullByDefault
@FieldsAreNonnullByDefault
public class ShapedEnergyTransferRecipe extends ShapedRecipe {
    public static final RecipeSerializer<ShapedEnergyTransferRecipe> SERIALIZER = new Serializer();

    private CraftingContainer craftingContainer;

    @Getter
    private final Ingredient chargeIngredient;
    @Getter
    private final boolean transferMaxCharge;
    @Getter
    private final boolean overrideCharge;


    public ShapedEnergyTransferRecipe(ResourceLocation id, String group, int width, int height, Ingredient chargeIngredient, boolean overrideCharge, boolean transferMaxCharge, NonNullList<Ingredient> recipeItems, ItemStack result) {
        super(id, group, CraftingBookCategory.MISC, width, height, recipeItems, result);
        this.chargeIngredient = chargeIngredient;
        this.transferMaxCharge = transferMaxCharge;
        this.overrideCharge = overrideCharge;
    }

    @Override
    public boolean matches(CraftingContainer inv, Level level) {
        this.craftingContainer = inv;
        return super.matches(inv, level);
    }

    @Override
    public ItemStack getResultItem(RegistryAccess registryManager) {
        long maxCharge = 0L;
        long charge = 0L;
        ItemStack resultStack = super.getResultItem(registryManager);
        for (ItemStack chargeStack : chargeIngredient.getItems()) {
            for (int i = 0; i < craftingContainer.getContainerSize(); i++) {
                if (ItemStack.isSameItem(craftingContainer.getItem(i), chargeStack)) {
                    ItemStack stack = craftingContainer.getItem(i);
                    IElectricItem electricItem = GTCapabilityHelper.getElectricItem(stack);
                    if (electricItem != null) {
                        maxCharge += electricItem.getMaxCharge();
                        charge += electricItem.getCharge();
                        resultStack.getOrCreateTag().putLong("MaxCharge", maxCharge);
                        resultStack.getOrCreateTag().putLong("Charge", charge);
                        return resultStack;
                    }
                }
            }
        }
        return resultStack;
    }



    public static class Serializer implements RecipeSerializer<ShapedEnergyTransferRecipe> {

        @Override
        public ShapedEnergyTransferRecipe fromJson(ResourceLocation recipeId, JsonObject json) {
            String string = GsonHelper.getAsString(json, "group", "");
            Map<String, Ingredient> map = ShapedRecipeInvoker.callKeyFromJson(GsonHelper.getAsJsonObject(json, "key"));
            String[] strings = ShapedRecipeInvoker.callPatternFromJson(GsonHelper.getAsJsonArray(json, "pattern"));
            int i = strings[0].length();
            int j = strings.length;
            NonNullList<Ingredient> nonNullList = ShapedRecipeInvoker.callDissolvePattern(strings, map, i, j);
            boolean overrideCharge = GsonHelper.getAsBoolean(json, "overrideCharge");
            boolean transferMaxCharge = GsonHelper.getAsBoolean(json, "transferMaxCharge");
            Ingredient chargeIngredient = Ingredient.fromJson(GsonHelper.getAsJsonObject(json, "chargeIngredient"));
            ItemStack itemStack = ShapedEnergyTransferRecipe.itemStackFromJson(GsonHelper.getAsJsonObject(json, "result"));
            return new ShapedEnergyTransferRecipe(recipeId, string, i, j, chargeIngredient, overrideCharge, transferMaxCharge, nonNullList, itemStack);
        }

        @Override
        public ShapedEnergyTransferRecipe fromNetwork(ResourceLocation recipeId, FriendlyByteBuf buffer) {
            int i = buffer.readVarInt();
            int j = buffer.readVarInt();
            boolean overrideCharge = buffer.readBoolean();
            boolean transferMaxCharge = buffer.readBoolean();
            Ingredient chargeIngredient = Ingredient.fromNetwork(buffer);
            String string = buffer.readUtf();
            NonNullList<Ingredient> nonNullList = NonNullList.withSize(i * j, Ingredient.EMPTY);
            nonNullList.replaceAll(ignored -> Ingredient.fromNetwork(buffer));
            ItemStack itemStack = buffer.readItem();
            return new ShapedEnergyTransferRecipe(recipeId, string, i, j, chargeIngredient, overrideCharge, transferMaxCharge, nonNullList, itemStack);
        }

        @Override
        public void toNetwork(FriendlyByteBuf buffer, ShapedEnergyTransferRecipe recipe) {
            buffer.writeVarInt(recipe.getWidth());
            buffer.writeVarInt(recipe.getHeight());
            buffer.writeBoolean(recipe.isOverrideCharge());
            buffer.writeBoolean(recipe.isTransferMaxCharge());
            recipe.getChargeIngredient().toNetwork(buffer);
            buffer.writeUtf(recipe.getGroup());
            for (Ingredient ingredient : recipe.getIngredients()) {
                ingredient.toNetwork(buffer);
            }
            buffer.writeItem(recipe.getResultItem(GTRegistries.builtinRegistry()));
        }
    }

}
