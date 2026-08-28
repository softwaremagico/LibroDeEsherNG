package com.softwaremagico.librodeesher.weapon;

/**
 * Broad weapon category, matching both the legacy {@code armas/<Tipo>.txt} file names and the
 * "Armas·&lt;Tipo&gt;" {@link com.softwaremagico.librodeesher.category.Category} (see
 * {@link #getCategoryId()}).
 *
 * <p>Serialized to XML using the plain (English) enum constant name; {@link #fromTag(String)} is
 * only used by {@code WeaponMigrationTool} to resolve the original Spanish file name. The Spanish
 * {@code tag} is never used at runtime; {@link #getCategoryId()} returns a fixed, English id
 * (matching the corresponding "Armas·&lt;Tipo&gt;" category's real id in {@code categories.xml},
 * verified against the migrated data) instead of deriving one from the Spanish tag.</p>
 */
public enum WeaponType {

    EDGE("Filo", "weaponsEdged"),
    BLUNT("Contundentes", "weaponsBlunt"),
    THROWING("Arrojadizas", "weaponsThrown"),
    PROJECTILE("Proyectiles", "weaponsMissile"),
    HANDLE("Asta", "weaponsPolearm"),
    TWO_HANDS("2manos", "weaponsTwoHanded"),
    SIEGE("Artillería", "weaponsSiege"),
    FIREARM_ONE_HAND("Fuego 1mano", "weaponsFirearmOneHanded"),
    FIREARM_TWO_HANDS("Fuego 2manos", "weaponsFirearmTwoHanded");

    private final String tag;
    private final String categoryId;

    WeaponType(String tag, String categoryId) {
        this.tag = tag;
        this.categoryId = categoryId;
    }

    /** Id of the skill category that groups every weapon of this type. */
    public String getCategoryId() {
        return categoryId;
    }

    /** Resolves the weapon type from the legacy {@code armas/<Tipo>.txt} file name (e.g. "Filo"). */
    public static WeaponType fromTag(String tag) {
        final String normalized = tag.trim();
        for (final WeaponType type : values()) {
            if (type.tag.equalsIgnoreCase(normalized)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown weapon type tag '" + tag + "'.");
    }
}
