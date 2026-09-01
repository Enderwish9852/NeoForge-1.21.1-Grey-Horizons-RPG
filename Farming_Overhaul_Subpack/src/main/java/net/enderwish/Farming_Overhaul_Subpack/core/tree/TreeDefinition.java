package net.enderwish.Farming_Overhaul_Subpack.core.tree;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

/**
 * TreeDefinition — Tree DNA (v3)
 *
 * Two supported architectures:
 *
 * DELIQUESCENT (oak, birch, acacia, dark_oak, jungle, cherry, mangrove)
 *   Trunk forks near top into primary limbs. Uses the full
 *   trunk/primary/secondary/twig hierarchy.
 *
 * EXCURRENT (spruce)
 *   Single central leader runs full height. Horizontal branch whorls
 *   emerge at regular intervals. Branches taper in length toward top,
 *   producing the classic conical/spire crown.
 *   Uses whorl_* parameters instead of primary/secondary/twig.
 */
public class TreeDefinition {

    // ── Identity ──────────────────────────────────────────────────────────────
    private final String species;
    private final String logBlock;
    private final String leafBlock;
    private final String fruitBlock;
    private final String fruitItem;

    public enum SeasonalType { TEMPERATE, MEDITERRANEAN, TROPICAL }
    private final SeasonalType seasonalType;

    public enum TreeArchitecture { DELIQUESCENT, EXCURRENT }
    private final TreeArchitecture architecture;

    // ── Trunk ─────────────────────────────────────────────────────────────────
    private final int trunkThicknessLevel;
    private final int trunkHeightMin;
    private final int trunkHeightMax;
    private final float trunkSplitFractionMin;
    private final float trunkSplitFractionMax;

    // ── Primary limbs (DELIQUESCENT only) ────────────────────────────────────
    private final int primaryThicknessLevel;
    private final int primaryCountMin;
    private final int primaryCountMax;
    private final int primaryLengthMin;
    private final int primaryLengthMax;
    private final float primarySpreadMin;
    private final float primarySpreadMax;
    private final float primaryUpwardLiftMin;
    private final float primaryUpwardLiftMax;

    // ── Secondary branches (DELIQUESCENT only) ────────────────────────────────
    private final int secondaryThicknessLevel;
    private final int secondaryCountMin;
    private final int secondaryCountMax;
    private final int secondaryLengthMin;
    private final int secondaryLengthMax;
    private final float secondaryInwardPullMin;
    private final float secondaryInwardPullMax;
    private final float secondaryUpwardLiftMin;
    private final float secondaryUpwardLiftMax;
    private final float secondaryZigzagMin;
    private final float secondaryZigzagMax;

    // ── Twigs (DELIQUESCENT only) ─────────────────────────────────────────────
    private final int twigThicknessLevel;
    private final int twigCountMin;
    private final int twigCountMax;
    private final int twigLengthMin;
    private final int twigLengthMax;
    private final float twigVerticalityMin;
    private final float twigVerticalityMax;
    private final boolean twigsDroop;

    // ── Whorl parameters (EXCURRENT only) ────────────────────────────────────
    // Whorls = horizontal branch sets at regular intervals up the trunk.
    // Each successive whorl is shorter, producing the conical shape.
    private final int whorlThicknessLevel;
    private final int whorlSpacingMin;         // blocks between whorls
    private final int whorlSpacingMax;
    private final int whorlBranchCountMin;     // branches per whorl
    private final int whorlBranchCountMax;
    private final int whorlBaseLengthMin;      // branch length at bottom whorl
    private final int whorlBaseLengthMax;
    private final float whorlLengthTaper;      // multiplier per whorl going up
    private final float whorlSpread;           // 1.0=horizontal, 0.0=vertical
    private final boolean whorlDroop;          // tips droop downward
    private final int whorlSubBranchThickness;

    // ── Canopy ────────────────────────────────────────────────────────────────
    private final int canopyClusterRadiusMin;
    private final int canopyClusterRadiusMax;
    private final float leafDensityMin;
    private final float leafDensityMax;
    private final float canopyTopBias;
    private final boolean canopyCoversFullBranch; // excurrent: leaves all over

    // ── Fruit ─────────────────────────────────────────────────────────────────
    private final float fruitSpawnRate;
    private final float fruitGrowthChance;
    private final float fruitDropChance;
    private final List<String> fruitingSeasons;

    // ── World gen ─────────────────────────────────────────────────────────────
    private final List<String> biomeTags;

    private TreeDefinition(Builder b) {
        this.species                  = b.species;
        this.logBlock                 = b.logBlock;
        this.leafBlock                = b.leafBlock;
        this.fruitBlock               = b.fruitBlock;
        this.fruitItem                = b.fruitItem;
        this.seasonalType             = b.seasonalType;
        this.architecture             = b.architecture;
        this.trunkThicknessLevel      = b.trunkThicknessLevel;
        this.trunkHeightMin           = b.trunkHeightMin;
        this.trunkHeightMax           = b.trunkHeightMax;
        this.trunkSplitFractionMin    = b.trunkSplitFractionMin;
        this.trunkSplitFractionMax    = b.trunkSplitFractionMax;
        this.primaryThicknessLevel    = b.primaryThicknessLevel;
        this.primaryCountMin          = b.primaryCountMin;
        this.primaryCountMax          = b.primaryCountMax;
        this.primaryLengthMin         = b.primaryLengthMin;
        this.primaryLengthMax         = b.primaryLengthMax;
        this.primarySpreadMin         = b.primarySpreadMin;
        this.primarySpreadMax         = b.primarySpreadMax;
        this.primaryUpwardLiftMin     = b.primaryUpwardLiftMin;
        this.primaryUpwardLiftMax     = b.primaryUpwardLiftMax;
        this.secondaryThicknessLevel  = b.secondaryThicknessLevel;
        this.secondaryCountMin        = b.secondaryCountMin;
        this.secondaryCountMax        = b.secondaryCountMax;
        this.secondaryLengthMin       = b.secondaryLengthMin;
        this.secondaryLengthMax       = b.secondaryLengthMax;
        this.secondaryInwardPullMin   = b.secondaryInwardPullMin;
        this.secondaryInwardPullMax   = b.secondaryInwardPullMax;
        this.secondaryUpwardLiftMin   = b.secondaryUpwardLiftMin;
        this.secondaryUpwardLiftMax   = b.secondaryUpwardLiftMax;
        this.secondaryZigzagMin       = b.secondaryZigzagMin;
        this.secondaryZigzagMax       = b.secondaryZigzagMax;
        this.twigThicknessLevel       = b.twigThicknessLevel;
        this.twigCountMin             = b.twigCountMin;
        this.twigCountMax             = b.twigCountMax;
        this.twigLengthMin            = b.twigLengthMin;
        this.twigLengthMax            = b.twigLengthMax;
        this.twigVerticalityMin       = b.twigVerticalityMin;
        this.twigVerticalityMax       = b.twigVerticalityMax;
        this.twigsDroop               = b.twigsDroop;
        this.whorlThicknessLevel      = b.whorlThicknessLevel;
        this.whorlSpacingMin          = b.whorlSpacingMin;
        this.whorlSpacingMax          = b.whorlSpacingMax;
        this.whorlBranchCountMin      = b.whorlBranchCountMin;
        this.whorlBranchCountMax      = b.whorlBranchCountMax;
        this.whorlBaseLengthMin       = b.whorlBaseLengthMin;
        this.whorlBaseLengthMax       = b.whorlBaseLengthMax;
        this.whorlLengthTaper         = b.whorlLengthTaper;
        this.whorlSpread              = b.whorlSpread;
        this.whorlDroop               = b.whorlDroop;
        this.whorlSubBranchThickness  = b.whorlSubBranchThickness;
        this.canopyClusterRadiusMin   = b.canopyClusterRadiusMin;
        this.canopyClusterRadiusMax   = b.canopyClusterRadiusMax;
        this.leafDensityMin           = b.leafDensityMin;
        this.leafDensityMax           = b.leafDensityMax;
        this.canopyTopBias            = b.canopyTopBias;
        this.canopyCoversFullBranch   = b.canopyCoversFullBranch;
        this.fruitSpawnRate           = b.fruitSpawnRate;
        this.fruitGrowthChance        = b.fruitGrowthChance;
        this.fruitDropChance          = b.fruitDropChance;
        this.fruitingSeasons          = b.fruitingSeasons;
        this.biomeTags                = b.biomeTags;
    }

    // ── JSON parsing ──────────────────────────────────────────────────────────

    public static TreeDefinition fromJson(String species, JsonObject json) {
        Builder b = new Builder(species);

        b.logBlock   = json.get("log_block").getAsString();
        b.leafBlock  = json.get("leaf_block").getAsString();
        b.fruitBlock = json.has("fruit_block") ? json.get("fruit_block").getAsString() : null;
        b.fruitItem  = json.has("fruit_item")  ? json.get("fruit_item").getAsString()  : null;
        b.seasonalType = SeasonalType.valueOf(
                json.get("seasonal_type").getAsString().toUpperCase());
        b.architecture = json.has("architecture")
                ? TreeArchitecture.valueOf(json.get("architecture").getAsString().toUpperCase())
                : TreeArchitecture.DELIQUESCENT;

        JsonObject trunk = json.getAsJsonObject("trunk");
        b.trunkThicknessLevel   = trunk.get("thickness_level").getAsInt();
        b.trunkHeightMin        = trunk.get("height_min").getAsInt();
        b.trunkHeightMax        = trunk.get("height_max").getAsInt();
        b.trunkSplitFractionMin = trunk.get("split_fraction_min").getAsFloat();
        b.trunkSplitFractionMax = trunk.get("split_fraction_max").getAsFloat();

        if (b.architecture == TreeArchitecture.DELIQUESCENT) {
            JsonObject primary = json.getAsJsonObject("primary_limbs");
            b.primaryThicknessLevel = primary.get("thickness_level").getAsInt();
            b.primaryCountMin       = primary.get("count_min").getAsInt();
            b.primaryCountMax       = primary.get("count_max").getAsInt();
            b.primaryLengthMin      = primary.get("length_min").getAsInt();
            b.primaryLengthMax      = primary.get("length_max").getAsInt();
            b.primarySpreadMin      = primary.get("spread_min").getAsFloat();
            b.primarySpreadMax      = primary.get("spread_max").getAsFloat();
            b.primaryUpwardLiftMin  = primary.get("upward_lift_min").getAsFloat();
            b.primaryUpwardLiftMax  = primary.get("upward_lift_max").getAsFloat();

            JsonObject secondary = json.getAsJsonObject("secondary_branches");
            b.secondaryThicknessLevel = secondary.get("thickness_level").getAsInt();
            b.secondaryCountMin       = secondary.get("count_per_parent_min").getAsInt();
            b.secondaryCountMax       = secondary.get("count_per_parent_max").getAsInt();
            b.secondaryLengthMin      = secondary.get("length_min").getAsInt();
            b.secondaryLengthMax      = secondary.get("length_max").getAsInt();
            b.secondaryInwardPullMin  = secondary.get("inward_pull_min").getAsFloat();
            b.secondaryInwardPullMax  = secondary.get("inward_pull_max").getAsFloat();
            b.secondaryUpwardLiftMin  = secondary.get("upward_lift_min").getAsFloat();
            b.secondaryUpwardLiftMax  = secondary.get("upward_lift_max").getAsFloat();
            b.secondaryZigzagMin      = secondary.get("zigzag_min").getAsFloat();
            b.secondaryZigzagMax      = secondary.get("zigzag_max").getAsFloat();

            JsonObject twigs = json.getAsJsonObject("twigs");
            b.twigThicknessLevel  = twigs.get("thickness_level").getAsInt();
            b.twigCountMin        = twigs.get("count_per_parent_min").getAsInt();
            b.twigCountMax        = twigs.get("count_per_parent_max").getAsInt();
            b.twigLengthMin       = twigs.get("length_min").getAsInt();
            b.twigLengthMax       = twigs.get("length_max").getAsInt();
            b.twigVerticalityMin  = twigs.get("verticality_min").getAsFloat();
            b.twigVerticalityMax  = twigs.get("verticality_max").getAsFloat();
            b.twigsDroop          = twigs.has("droop") && twigs.get("droop").getAsBoolean();
        } else {
            JsonObject whorl = json.getAsJsonObject("whorls");
            b.whorlThicknessLevel     = whorl.get("thickness_level").getAsInt();
            b.whorlSpacingMin         = whorl.get("spacing_min").getAsInt();
            b.whorlSpacingMax         = whorl.get("spacing_max").getAsInt();
            b.whorlBranchCountMin     = whorl.get("branch_count_min").getAsInt();
            b.whorlBranchCountMax     = whorl.get("branch_count_max").getAsInt();
            b.whorlBaseLengthMin      = whorl.get("base_length_min").getAsInt();
            b.whorlBaseLengthMax      = whorl.get("base_length_max").getAsInt();
            b.whorlLengthTaper        = whorl.get("length_taper").getAsFloat();
            b.whorlSpread             = whorl.get("spread").getAsFloat();
            b.whorlDroop              = whorl.has("droop") && whorl.get("droop").getAsBoolean();
            b.whorlSubBranchThickness = whorl.has("sub_branch_thickness")
                    ? whorl.get("sub_branch_thickness").getAsInt() : 1;
        }

        JsonObject canopy = json.getAsJsonObject("canopy");
        b.canopyClusterRadiusMin  = canopy.get("cluster_radius_min").getAsInt();
        b.canopyClusterRadiusMax  = canopy.get("cluster_radius_max").getAsInt();
        b.leafDensityMin          = canopy.get("leaf_density_min").getAsFloat();
        b.leafDensityMax          = canopy.get("leaf_density_max").getAsFloat();
        b.canopyTopBias           = canopy.get("top_bias").getAsFloat();
        b.canopyCoversFullBranch  = canopy.has("covers_full_branch")
                && canopy.get("covers_full_branch").getAsBoolean();

        if (json.has("fruit")) {
            JsonObject fruit = json.getAsJsonObject("fruit");
            b.fruitSpawnRate    = fruit.get("spawn_rate").getAsFloat();
            b.fruitGrowthChance = fruit.get("growth_chance").getAsFloat();
            b.fruitDropChance   = fruit.get("drop_chance").getAsFloat();
            for (JsonElement e : fruit.getAsJsonArray("fruiting_seasons"))
                b.fruitingSeasons.add(e.getAsString());
        }

        if (json.has("biome_tags"))
            for (JsonElement e : json.getAsJsonArray("biome_tags"))
                b.biomeTags.add(e.getAsString());

        return new TreeDefinition(b);
    }

    // ── Randomized getters ────────────────────────────────────────────────────

    public int randomTrunkHeight(net.minecraft.util.RandomSource r)          { return randomInt(r, trunkHeightMin, trunkHeightMax); }
    public float randomSplitFraction(net.minecraft.util.RandomSource r)      { return randomFloat(r, trunkSplitFractionMin, trunkSplitFractionMax); }
    public int randomPrimaryCount(net.minecraft.util.RandomSource r)         { return randomInt(r, primaryCountMin, primaryCountMax); }
    public int randomPrimaryLength(net.minecraft.util.RandomSource r)        { return randomInt(r, primaryLengthMin, primaryLengthMax); }
    public float randomPrimarySpread(net.minecraft.util.RandomSource r)      { return randomFloat(r, primarySpreadMin, primarySpreadMax); }
    public float randomPrimaryUpwardLift(net.minecraft.util.RandomSource r)  { return randomFloat(r, primaryUpwardLiftMin, primaryUpwardLiftMax); }
    public int randomSecondaryCount(net.minecraft.util.RandomSource r)       { return randomInt(r, secondaryCountMin, secondaryCountMax); }
    public int randomSecondaryLength(net.minecraft.util.RandomSource r)      { return randomInt(r, secondaryLengthMin, secondaryLengthMax); }
    public float randomSecondaryInwardPull(net.minecraft.util.RandomSource r){ return randomFloat(r, secondaryInwardPullMin, secondaryInwardPullMax); }
    public float randomSecondaryUpwardLift(net.minecraft.util.RandomSource r){ return randomFloat(r, secondaryUpwardLiftMin, secondaryUpwardLiftMax); }
    public float randomSecondaryZigzag(net.minecraft.util.RandomSource r)    { return randomFloat(r, secondaryZigzagMin, secondaryZigzagMax); }
    public int randomTwigCount(net.minecraft.util.RandomSource r)            { return randomInt(r, twigCountMin, twigCountMax); }
    public int randomTwigLength(net.minecraft.util.RandomSource r)           { return randomInt(r, twigLengthMin, twigLengthMax); }
    public float randomTwigVerticality(net.minecraft.util.RandomSource r)    { return randomFloat(r, twigVerticalityMin, twigVerticalityMax); }
    public int randomWhorlSpacing(net.minecraft.util.RandomSource r)         { return randomInt(r, whorlSpacingMin, whorlSpacingMax); }
    public int randomWhorlBranchCount(net.minecraft.util.RandomSource r)     { return randomInt(r, whorlBranchCountMin, whorlBranchCountMax); }
    public int randomWhorlBaseLength(net.minecraft.util.RandomSource r)      { return randomInt(r, whorlBaseLengthMin, whorlBaseLengthMax); }
    public int randomCanopyClusterRadius(net.minecraft.util.RandomSource r)  { return randomInt(r, canopyClusterRadiusMin, canopyClusterRadiusMax); }
    public float randomLeafDensity(net.minecraft.util.RandomSource r)        { return randomFloat(r, leafDensityMin, leafDensityMax); }

    // ── Plain getters ─────────────────────────────────────────────────────────

    public String getSpecies()              { return species; }
    public String getLogBlock()             { return logBlock; }
    public String getLeafBlock()            { return leafBlock; }
    public String getFruitBlock()           { return fruitBlock; }
    public String getFruitItem()            { return fruitItem; }
    public SeasonalType getSeasonalType()   { return seasonalType; }
    public TreeArchitecture getArchitecture(){ return architecture; }
    public boolean hasFruit()               { return fruitBlock != null; }
    public float getFruitSpawnRate()        { return fruitSpawnRate; }
    public float getFruitGrowthChance()     { return fruitGrowthChance; }
    public float getFruitDropChance()       { return fruitDropChance; }
    public List<String> getFruitingSeasons(){ return fruitingSeasons; }
    public List<String> getBiomeTags()      { return biomeTags; }
    public float getCanopyTopBias()         { return canopyTopBias; }
    public boolean canopyCoversFullBranch() { return canopyCoversFullBranch; }
    public int getTrunkThicknessLevel()     { return trunkThicknessLevel; }
    public int getPrimaryThicknessLevel()   { return primaryThicknessLevel; }
    public int getSecondaryThicknessLevel() { return secondaryThicknessLevel; }
    public int getTwigThicknessLevel()      { return twigThicknessLevel; }
    public int getWhorlThicknessLevel()     { return whorlThicknessLevel; }
    public float getWhorlLengthTaper()      { return whorlLengthTaper; }
    public float getWhorlSpread()           { return whorlSpread; }
    public boolean whorlDroop()             { return whorlDroop; }
    public int getWhorlSubBranchThickness() { return whorlSubBranchThickness; }

    public boolean isEvergreen()  { return seasonalType == SeasonalType.MEDITERRANEAN || seasonalType == SeasonalType.TROPICAL; }
    public boolean hasBlossoms()  { return seasonalType == SeasonalType.TEMPERATE; }
    public boolean twigsDroop()   { return twigsDroop; }
    public boolean fruitsInSeason(String season) { return fruitingSeasons.contains(season.toUpperCase()); }

    private static int randomInt(net.minecraft.util.RandomSource r, int min, int max) {
        if (max <= min) return min;
        return min + r.nextInt(max - min + 1);
    }
    private static float randomFloat(net.minecraft.util.RandomSource r, float min, float max) {
        if (max <= min) return min;
        return min + r.nextFloat() * (max - min);
    }

    private static class Builder {
        String species;
        String logBlock, leafBlock;
        String fruitBlock = null, fruitItem = null;
        SeasonalType seasonalType = SeasonalType.TEMPERATE;
        TreeArchitecture architecture = TreeArchitecture.DELIQUESCENT;
        int trunkThicknessLevel = 6;
        int trunkHeightMin = 3, trunkHeightMax = 5;
        float trunkSplitFractionMin = 0.85f, trunkSplitFractionMax = 1.0f;
        int primaryThicknessLevel = 5;
        int primaryCountMin = 3, primaryCountMax = 5;
        int primaryLengthMin = 3, primaryLengthMax = 4;
        float primarySpreadMin = 1.0f, primarySpreadMax = 1.5f;
        float primaryUpwardLiftMin = 0.3f, primaryUpwardLiftMax = 0.5f;
        int secondaryThicknessLevel = 3;
        int secondaryCountMin = 2, secondaryCountMax = 3;
        int secondaryLengthMin = 2, secondaryLengthMax = 3;
        float secondaryInwardPullMin = 0.3f, secondaryInwardPullMax = 0.6f;
        float secondaryUpwardLiftMin = 0.7f, secondaryUpwardLiftMax = 1.0f;
        float secondaryZigzagMin = 0.2f, secondaryZigzagMax = 0.4f;
        int twigThicknessLevel = 1;
        int twigCountMin = 2, twigCountMax = 4;
        int twigLengthMin = 1, twigLengthMax = 2;
        float twigVerticalityMin = 0.7f, twigVerticalityMax = 0.95f;
        boolean twigsDroop = false;
        int whorlThicknessLevel = 4;
        int whorlSpacingMin = 2, whorlSpacingMax = 3;
        int whorlBranchCountMin = 3, whorlBranchCountMax = 5;
        int whorlBaseLengthMin = 4, whorlBaseLengthMax = 6;
        float whorlLengthTaper = 0.8f;
        float whorlSpread = 0.95f;
        boolean whorlDroop = false;
        int whorlSubBranchThickness = 1;
        int canopyClusterRadiusMin = 2, canopyClusterRadiusMax = 3;
        float leafDensityMin = 0.85f, leafDensityMax = 0.95f;
        float canopyTopBias = 0.65f;
        boolean canopyCoversFullBranch = false;
        float fruitSpawnRate = 0f, fruitGrowthChance = 0f, fruitDropChance = 0f;
        List<String> fruitingSeasons = new ArrayList<>();
        List<String> biomeTags = new ArrayList<>();
        Builder(String species) { this.species = species; }
    }
}