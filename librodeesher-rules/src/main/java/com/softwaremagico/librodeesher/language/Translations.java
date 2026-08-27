package com.softwaremagico.librodeesher.language;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Produces an English translation for the Spanish-only text found in every legacy rulebook file.
 *
 * <p>None of the original "LibroDeEsher" data ever had an English version (unlike ThinkMachine-4E,
 * whose XML always carries both {@code <es>}/{@code <en>} from the start): every English string in
 * the migrated XML is produced once, by this class, when the {@code *MigrationTool}s run.</p>
 *
 * <p>Translation strategy, in order:</p>
 * <ol>
 *     <li>An exact-phrase lookup ({@link #PHRASES}), covering every category name, characteristic,
 *     weapon type, magic realm and the most common recurring skill/perk/spell names.</li>
 *     <li>Splitting on {@code "·"} (used throughout the data as "Category·Subcategory") and
 *     translating each side independently, joined back with {@code ": "}.</li>
 *     <li>A word-by-word fallback ({@link #WORDS}) for any phrase not covered above: every
 *     recognized Spanish word/particle is replaced, punctuation and capitalization are preserved,
 *     and any word not in the dictionary is left as-is (better an occasional untranslated word than
 *     a silently wrong one).</li>
 * </ol>
 *
 * <p>Given the sheer number of distinct names across every rulebook (categories, skills, perks,
 * weapons, trainings, professions, spell lists — well over a thousand combined), full, idiomatic
 * hand-translation of every single one is not realistic; this class instead provides a systematic,
 * reviewable, and easily extended translation, favouring accuracy for the terms that are reused the
 * most (categories, characteristics, weapon/armor types) over the long tail of specific spell/skill
 * names.</p>
 */
public final class Translations {

    /** Exact-phrase translations, checked case-insensitively before any word-splitting. */
    private static final Map<String, String> PHRASES = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

    /** Word/particle-level fallback translations, checked case-insensitively. */
    private static final Map<String, String> WORDS = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

    static {
        registerPhrases();
        registerWords();
    }

    private Translations() {
        // Utility class.
    }

    /** Translates {@code spanish} to English using the strategy described in the class javadoc. */
    public static String toEnglish(String spanish) {
        if (spanish == null || spanish.isBlank()) {
            return spanish;
        }
        final String exact = PHRASES.get(spanish.trim());
        if (exact != null) {
            return exact;
        }
        if (spanish.contains("·")) {
            final String[] parts = spanish.split("·", 2);
            return toEnglish(parts[0].trim()) + ": " + toEnglish(parts[1].trim());
        }
        if (spanish.contains(": ")) {
            // "Revolver: Colt Trooper" -> translate the weapon-type prefix; the suffix is translated
            // too, but a proper noun (e.g. a firearm model) simply has no dictionary entry and comes
            // back unchanged via the word-by-word fallback.
            final String[] parts = spanish.split(": ", 2);
            return toEnglish(parts[0].trim()) + ": " + toEnglish(parts[1].trim());
        }
        return translateWords(spanish);
    }

    /**
     * Converts an already-English phrase into a {@code camelCase} identifier, e.g.
     * {@code "Light Armor"} -&gt; {@code "lightArmor"}, {@code "Two-Handed"} -&gt; {@code "twoHanded"}.
     * Any run of non letter/digit characters is treated as a word boundary and dropped.
     */
    public static String toId(String english) {
        if (english == null) {
            return null;
        }
        final StringBuilder id = new StringBuilder();
        boolean capitalizeNext = false;
        boolean first = true;
        for (final char c : english.toCharArray()) {
            if (Character.isLetterOrDigit(c)) {
                if (first) {
                    id.append(Character.toLowerCase(c));
                    first = false;
                } else if (capitalizeNext) {
                    id.append(Character.toUpperCase(c));
                    capitalizeNext = false;
                } else {
                    id.append(Character.toLowerCase(c));
                }
            } else {
                capitalizeNext = true;
            }
        }
        return id.toString();
    }

    /** Shorthand for {@code toId(toEnglish(spanish))}: the id a Spanish name would be assigned. */
    public static String toEnglishId(String spanish) {
        return toId(toEnglish(spanish));
    }

    /** Splits {@code text} into words (keeping punctuation attached) and translates each one. */
    private static String translateWords(String text) {
        final Pattern wordPattern = Pattern.compile("[\\p{L}][\\p{L}'-]*");
        final Matcher matcher = wordPattern.matcher(text);
        final StringBuilder result = new StringBuilder();
        int lastEnd = 0;
        while (matcher.find()) {
            result.append(text, lastEnd, matcher.start());
            result.append(translateWord(matcher.group()));
            lastEnd = matcher.end();
        }
        result.append(text.substring(lastEnd));
        return result.toString();
    }

    private static String translateWord(String word) {
        final String exactPhrase = PHRASES.get(word);
        if (exactPhrase != null) {
            return matchCase(word, exactPhrase);
        }
        final String translated = WORDS.get(word);
        if (translated == null) {
            return word;
        }
        return matchCase(word, translated);
    }

    /** Capitalizes {@code translated} like {@code original} (all-caps, capitalized, or lowercase). */
    private static String matchCase(String original, String translated) {
        if (original.equals(original.toUpperCase()) && original.length() > 1) {
            return translated.toUpperCase();
        }
        if (Character.isUpperCase(original.charAt(0))) {
            return Character.toUpperCase(translated.charAt(0)) + translated.substring(1);
        }
        return translated;
    }

    private static void put(Map<String, String> map, String spanish, String english) {
        map.put(spanish, english);
    }

    private static void registerPhrases() {
        registerCharacteristics();
        registerWeaponAndCategoryGroups();
        registerCategories();
        registerMagicRealms();
        registerCommonSkillsAndPerks();
        registerWeaponNames();
        registerMartialArtsTerms();
        registerProfessionNames();
        registerExactOverrides();
    }

    private static void registerCharacteristics() {
        final Map<String, String> m = PHRASES;
        put(m, "Agilidad", "Agility");
        put(m, "Constitución", "Constitution");
        put(m, "Memoria", "Memory");
        put(m, "Razón", "Reasoning");
        put(m, "Autodisciplina", "Self Discipline");
        put(m, "Empatía", "Empathy");
        put(m, "Intuición", "Intuition");
        put(m, "Presencia", "Presence");
        put(m, "Fuerza", "Strength");
        put(m, "Rapidez", "Quickness");
        put(m, "Apariencia", "Appearance");
        put(m, "Indiferente", "Indifferent");
    }

    private static void registerWeaponAndCategoryGroups() {
        final Map<String, String> m = PHRASES;
        put(m, "Armas", "Weapons");
        put(m, "Armadura", "Armor");
        put(m, "Ligera", "Light");
        put(m, "Media", "Medium");
        put(m, "Pesada", "Heavy");
        put(m, "2manos", "Two-Handed");
        put(m, "Arrojadizas", "Thrown");
        put(m, "Artillería", "Siege");
        put(m, "Contundentes", "Blunt");
        put(m, "Asta", "Polearm");
        put(m, "Filo", "Edged");
        put(m, "Proyectiles", "Missile");
        put(m, "Fuego 1mano", "Firearm (One-Handed)");
        put(m, "Fuego 2manos", "Firearm (Two-Handed)");
        put(m, "Fuego de Supresión", "Suppression Fire");
        put(m, "Fuego Rápido", "Rapid Fire");
        put(m, "noimporta", "any");
    }

    private static void registerCategories() {
        final Map<String, String> m = PHRASES;
        put(m, "Arte", "Art");
        put(m, "Activo", "Performing");
        put(m, "Pasivo", "Creative");
        put(m, "Artes Marciales", "Martial Arts");
        put(m, "Golpes", "Strikes");
        put(m, "Barridos", "Sweeps");
        put(m, "Maniobras de Combate", "Combat Maneuvers");
        put(m, "Ataques Especiales", "Special Attacks");
        put(m, "Atletismo", "Athletics");
        put(m, "Gimnasia", "Gymnastics");
        put(m, "Potencia", "Power");
        put(m, "Resistencia", "Endurance");
        put(m, "Autocontrol", "Self Control");
        put(m, "Ciencia/Analítica", "Science/Analytic");
        put(m, "Básica", "Basic");
        put(m, "Especializada", "Specialized");
        put(m, "Comunicación", "Communication");
        put(m, "Conocimiento", "Lore");
        put(m, "General", "General");
        put(m, "Mágico", "Arcane");
        put(m, "Oscuro", "Dark");
        put(m, "Técnico", "Technical");
        put(m, "Defensas Especiales", "Special Defenses");
        put(m, "Desarrollo de Puntos de Poder", "Power Point Development");
        put(m, "Desarrollo Físico", "Physical Development");
        put(m, "Exteriores", "Outdoor");
        put(m, "Animales", "Animals");
        put(m, "Entorno", "Environment");
        put(m, "Hechizos Dirigidos", "Directed Spells");
        put(m, "Influencia", "Influence");
        put(m, "Manipulación del Poder", "Power Manipulation");
        put(m, "Oficios", "Crafts");
        put(m, "Percepción", "Perception");
        put(m, "Búsqueda", "Searching");
        put(m, "Perspicacia", "Insight");
        put(m, "Sentidos", "Senses");
        put(m, "Percepción de Poder", "Power Awareness");
        put(m, "Subterfugio", "Subterfuge");
        put(m, "Ataque", "Attack");
        put(m, "Mecánica", "Mechanics");
        put(m, "Sigilo", "Stealth");
        put(m, "Técnica/Comercio", "Technical/Trade");
        put(m, "Profesional", "Professional");
        put(m, "Vocacional", "Vocational");
        put(m, "Urbana", "Urban");
    }

    private static void registerMagicRealms() {
        final Map<String, String> m = PHRASES;
        put(m, "Canalización", "Channeling");
        put(m, "Esencia", "Essence");
        put(m, "Mentalismo", "Mentalism");
        put(m, "Psiónico", "Psionic");
        put(m, "Arcano", "Arcane");
        put(m, "Racial", "Racial");
        put(m, "Lista Abierta", "Open List");
        put(m, "Lista Cerrada", "Closed List");
    }

    private static void registerCommonSkillsAndPerks() {
        final Map<String, String> m = PHRASES;
        put(m, "Todos", "Everyone");
        put(m, "Ninguno", "None");
        put(m, "Ninguna", "None");
        put(m, "Estándar", "Standard");
        put(m, "Combinada", "Combined");
        put(m, "Limitada", "Limited");
        put(m, "Especial", "Special");
        put(m, "DPP", "PPD");
        put(m, "DF", "PD");
    }

    /** Weapon-type prefixes (e.g. "Revolver: Colt Trooper") and generic (non-brand) weapon names. */
    private static void registerWeaponNames() {
        final Map<String, String> m = PHRASES;
        put(m, "Ametralladora", "Machine Gun");
        put(m, "Pistola Automática", "Automatic Pistol");
        put(m, "Pistola Avancarga", "Muzzleloader Pistol");
        put(m, "Pistola Energética", "Energy Pistol");
        put(m, "Revolver", "Revolver");
        put(m, "Rifle", "Rifle");
        put(m, "Rifle de Avancarga", "Muzzleloader Rifle");
        put(m, "Rifle Energético", "Energy Rifle");
        put(m, "Escopeta", "Shotgun");
        put(m, "Subfusil", "Submachine Gun");

        put(m, "Abanico de Combate", "Combat Fan");
        put(m, "Alabarda", "Halberd");
        put(m, "Alfanje", "Scimitar");
        put(m, "Arco Compuesto", "Composite Bow");
        put(m, "Arco Corto", "Short Bow");
        put(m, "Arco Largo", "Long Bow");
        put(m, "Ariete", "Battering Ram");
        put(m, "Arma de Asta", "Polearm Weapon");
        put(m, "Arpón", "Harpoon");
        put(m, "Atrapa Hombres", "Man Catcher");
        put(m, "Ballesta de Mano", "Hand Crossbow");
        put(m, "Ballesta Ligera", "Light Crossbow");
        put(m, "Ballesta Pesada", "Heavy Crossbow");
        put(m, "Bastón Honda", "Staff Sling");
        put(m, "Bisarma", "Bisarme");
        put(m, "Boleadoras", "Bolas");
        put(m, "Cañon", "Cannon");
        put(m, "Catapulta", "Catapult");
        put(m, "Cayado", "Staff");
        put(m, "Cerbatana", "Blowgun");
        put(m, "Cimitarra", "Scimitar");
        put(m, "Cimitarra Larga", "Long Scimitar");
        put(m, "Cuchillo", "Knife");
        put(m, "Daga", "Dagger");
        put(m, "Daga Arrojadiza", "Throwing Dagger");
        put(m, "Dardo", "Dart");
        put(m, "Dardo con Cuerda", "Corded Dart");
        put(m, "Espada", "Sword");
        put(m, "Espada Bastarda", "Bastard Sword");
        put(m, "Espada Corta", "Short Sword");
        put(m, "Espada Grande", "Great Sword");
        put(m, "Espada Larga", "Long Sword");
        put(m, "Espadón", "Greatsword");
        put(m, "Estoque", "Rapier");
        put(m, "Estrella de la Mañana", "Morning Star");
        put(m, "Garfio de Tigre", "Tiger Hook");
        put(m, "Garrote", "Club");
        put(m, "Gato de Nueve Colas", "Cat o' Nine Tails");
        put(m, "Granada", "Grenade");
        put(m, "Hacha Bastarda", "Bastard Axe");
        put(m, "Hacha de Batalla", "Battle Axe");
        put(m, "Hacha de Mano", "Hand Axe");
        put(m, "Hacha Picuda", "Pick Axe");
        put(m, "Honda", "Sling");
        put(m, "Horca", "Pitchfork");
        put(m, "Jabalina", "Javelin");
        put(m, "Lanza", "Spear");
        put(m, "Lanza de Caballería", "Cavalry Lance");
        put(m, "Lanza Pesada", "Heavy Spear");
        put(m, "Látigo", "Whip");
        put(m, "Látigo de Acero", "Steel Whip");
        put(m, "Látigo Metálico", "Metal Whip");
        put(m, "Lazo", "Lasso");
        put(m, "Machete", "Machete");
        put(m, "Mangual", "Flail");
        put(m, "Martillo Cometa", "Meteor Hammer");
        put(m, "Martillo de Guerra", "War Hammer");
        put(m, "Martillo Lucerne", "Lucerne Hammer");
        put(m, "Maza", "Mace");
        put(m, "Pica", "Pike");
        put(m, "Pica de Guerra", "War Pike");
        put(m, "Red de Gladiador", "Gladiator Net");
        put(m, "Roca", "Rock");
        put(m, "Sable", "Saber");
        put(m, "Tridente", "Trident");
        put(m, "Vara de Tres Secciones", "Three-Section Staff");
    }

    /** Martial arts / "Chi Power" terms, common across the ArtesMarciales module. */
    private static void registerMartialArtsTerms() {
        final Map<String, String> m = PHRASES;
        put(m, "Poderes Chi", "Chi Power");
        put(m, "Estilo", "Style");
        put(m, "Grulla", "Crane");
        put(m, "Serpiente", "Snake");
        put(m, "Simio", "Ape");
        put(m, "Tigre", "Tiger");
        put(m, "Dragón", "Dragon");
        put(m, "Cobra", "Cobra");
        put(m, "Cobra Real", "King Cobra");
        put(m, "Túnica de Hierro", "Iron Robe");
        put(m, "Puño", "Fist");
        put(m, "Puños", "Fists");
        put(m, "Puño Envenenado", "Poisoned Fist");
        put(m, "Puño Elemental", "Elemental Fist");
        put(m, "Puño de Algodón", "Cotton Fist");
        put(m, "Hierro", "Iron");
        put(m, "Contínuo", "Continuous");
        put(m, "Contínuos", "Continuous");
        put(m, "Contacto Contínuo", "Continuous Contact");
        put(m, "Sombra", "Shadow");
        put(m, "Ataque Sin Sombra", "Shadowless Attack");
        put(m, "Distancia", "Distance");
        put(m, "Ataque a Distancia", "Ranged Attack");
        put(m, "Postura Invencible", "Invincible Stance");
        put(m, "Invulnerabilidad", "Invulnerability");
        put(m, "Resistir", "Resist");
        put(m, "Elementos", "Elements");
        put(m, "Resistir Elementos", "Resist Elements");
        put(m, "Dolor", "Pain");
        put(m, "Resistir el Dolor", "Resist Pain");
        put(m, "Resurgir", "Rebirth");
        put(m, "Fénix", "Phoenix");
        put(m, "Resurgir del Fénix", "Rebirth of the Phoenix");
        put(m, "Sentir Debilidad", "Sense Weakness");
        put(m, "Pies Ligeros", "Light Feet");
        put(m, "Mantener la Respiración", "Hold Breath");
        put(m, "Lagartija", "Gecko");
        put(m, "Trepar de la Lagartija", "Gecko Climb");
        put(m, "Buda", "Buddha");
        put(m, "Mano de Buda", "Hand of Buddha");
        put(m, "Tela", "Cloth");
        put(m, "Lanza de Tela", "Cloth Lance");
        put(m, "Golpe en Salto", "Jumping Strike");
        put(m, "Salto Fantástico", "Fantastic Leap");
        put(m, "Calor", "Heat");
        put(m, "Electricidad", "Electricity");
        put(m, "Frío", "Cold");
        put(m, "Impacto", "Impact");
        put(m, "Ocho Hadas Borrachas", "Eight Drunken Fairies");
        put(m, "Dos Instrumentos", "Two Weapons");
        put(m, "Mantis Religiosa", "Praying Mantis");
        put(m, "Wing Chun", "Wing Chun");
        put(m, "Golpe Nervioso", "Nerve Strike");
        put(m, "Lucha a Ciegas", "Blind Fighting");
        put(m, "Puntos Vitales", "Vital Points");
        put(m, "Arte Marcial", "Martial Art");
        put(m, "Avanzado", "Advanced");
    }

    /** Character professions (bounded, high-value set: ~40 across every module). */
    private static void registerProfessionNames() {
        final Map<String, String> m = PHRASES;
        put(m, "Mago", "Wizard");
        put(m, "Mago Principiante", "Novice Wizard");
        put(m, "Guerrero", "Warrior");
        put(m, "Luchador", "Fighter");
        put(m, "Bribón", "Rogue");
        put(m, "Bardo", "Bard");
        put(m, "Indagador", "Seeker");
        put(m, "Clérigo", "Cleric");
        put(m, "Sacerdote", "Priest");
        put(m, "Sacerdote Chamánico", "Shamanic Priest");
        put(m, "Explorador", "Scout");
        put(m, "Soldado", "Soldier");
        put(m, "Mercader", "Merchant");
        put(m, "Viajero", "Traveler");
        put(m, "Trotamundos", "Globetrotter");
        put(m, "Artesano", "Artisan");
        put(m, "Artista", "Artist");
        put(m, "Artista Marcial", "Martial Artist");
        put(m, "Asesino", "Assassin");
        put(m, "Aventurero", "Adventurer");
        put(m, "Berserker", "Berserker");
        put(m, "Caballero", "Knight");
        put(m, "Cazador", "Hunter");
        put(m, "Cortabolsas", "Cutpurse");
        put(m, "Desvalijador", "Burglar");
        put(m, "Detective", "Detective");
        put(m, "Diplomático", "Diplomat");
        put(m, "Doctor", "Doctor");
        put(m, "Erudito", "Scholar");
        put(m, "Erudito Aventurero", "Adventurous Scholar");
        put(m, "Erudito Enclaustrado", "Cloistered Scholar");
        put(m, "Espía", "Spy");
        put(m, "Expedicionario", "Expeditionary");
        put(m, "Experto en Armas", "Weapons Expert");
        put(m, "Fanático Religioso", "Religious Fanatic");
        put(m, "Filósofo", "Philosopher");
        put(m, "Guardia", "Guard");
        put(m, "Guardián", "Guardian");
        put(m, "Herbolario", "Herbalist");
        put(m, "Houri", "Houri");
        put(m, "Marino", "Sailor");
        put(m, "Mercenario", "Mercenary");
        put(m, "Ninja", "Ninja");
        put(m, "Salteador", "Bandit");
        put(m, "Timador", "Swindler");
        put(m, "Amigo de las Bestias", "Friend of the Beasts");
        put(m, "Elementalista", "Elementalist");
        put(m, "Ilusionista", "Illusionist");
        put(m, "Monje", "Monk");
        put(m, "Orco Gris", "Gray Orc");
        put(m, "Hechicero", "Sorcerer");
        put(m, "Hechicero de Fuego", "Fire Sorcerer");
        put(m, "Mago de la Luz", "Light Wizard");
        put(m, "Mago de la Tierra", "Earth Wizard");
        put(m, "Mago del Agua", "Water Wizard");
        put(m, "Mago del Fuego", "Fire Wizard");
    }

    /**
     * Exact-phrase corrections for compounds where a plain word-by-word translation produces the
     * wrong (Spanish) noun-adjective order or an otherwise awkward result.
     */
    private static void registerExactOverrides() {
        final Map<String, String> m = PHRASES;
        put(m, "Cuero Endurecido", "Hardened Leather");
        put(m, "Cuero Blando", "Soft Leather");
        put(m, "Cota de Mallas", "Chain Mail");
        put(m, "Improvisación Poética", "Poetic Improvisation");
        put(m, "Percepción del Entorno: Munición", "Environment Perception: Ammunition");
        put(m, "Adicción Ligera", "Minor Addiction");
        put(m, "Adicción Mínima", "Slight Addiction");
        put(m, "Promesa", "Vow");
        put(m, "Ansioso", "Anxious");
        put(m, "Regalo Mortal", "Deadly Gift");
        put(m, "Secreto", "Secret");
        put(m, "Sentido del Deber", "Sense of Duty");
        put(m, "Sentido de la Firmeza", "Sense of Firmness");
        put(m, "Reflejos de Combate", "Combat Reflexes");
        put(m, "Reserva de Veneno", "Venom Reserve");
        put(m, "Pulmones Poderosos", "Powerful Lungs");
        put(m, "Grito de Guerra", "War Cry");
        put(m, "Instinto de Supervivencia", "Survival Instinct");
        put(m, "Intolerante", "Intolerant");
        put(m, "Megalómano", "Megalomaniac");
        put(m, "Mal Genio", "Bad Temper");
        put(m, "Miedo a las Armaduras", "Fear of Armor");
        put(m, "Caballeroso", "Chivalrous");
        put(m, "Fuerza Incontrolable", "Uncontrollable Strength");
        put(m, "Falta de Alcance", "Lack of Range");
        put(m, "Piel Gruesa", "Thick Skin");
        put(m, "Piel Sensible", "Sensitive Skin");
        put(m, "Sin Equilibrio", "Off Balance");
        put(m, "Voz Poderosa", "Powerful Voice");
        put(m, "Visión Periférica", "Peripheral Vision");
        put(m, "Zoquete", "Clumsy");
        put(m, "Afán Nigromántico", "Necromantic Drive");
        put(m, "Afición Compulsiva", "Compulsive Hobby");
        put(m, "Ansia de Sangre", "Bloodlust");
        put(m, "Aspecto Único", "Unique Appearance");
        put(m, "Aura", "Aura");
        put(m, "Avaro", "Miser");
        put(m, "Brazo Poderoso", "Powerful Arm");
        put(m, "Capacidad Mágica", "Magical Ability");
        put(m, "Control Mental", "Mind Control");
        put(m, "Control Sobre el Arma", "Weapon Mastery");
        put(m, "Desaprovación de las Armas", "Weapon Disapproval");
        put(m, "Dotes de Mando", "Leadership Skills");
        put(m, "Duplicación", "Duplication");
        put(m, "Edad", "Old Age");
        put(m, "Elástico", "Elastic");
        put(m, "Escéptico", "Skeptic");
        put(m, "Experimentado", "Experienced");
        put(m, "Experto en Hierbas", "Herb Expert");
        put(m, "Hombre de Campo", "Man of the Field");
        put(m, "Infravisión", "Infravision");
        put(m, "Jinete Nato", "Born Rider");
        put(m, "Juego de Muñecas", "Wrist Play");
        put(m, "Lazos Etéreos", "Ethereal Bonds");
        put(m, "Maldición de las Armas", "Weapon Curse");
        put(m, "Mente sobre Material", "Mind over Matter");
        put(m, "Prototipo", "Prototype");
        put(m, "Resistencia al Dolor", "Pain Resistance");
        put(m, "Vampiro", "Vampire");
        put(m, "Habilidades de Tiempo", "Time Skills");
    }

    private static void registerWords() {
        final Map<String, String> m = WORDS;
        // Perk/training grade suffixes, e.g. "Piel Gruesa (Máximo)".
        put(m, "Máximo", "Maximum");
        put(m, "Mayor", "Major");
        put(m, "Menor", "Minor");
        put(m, "Mínimo", "Minimum");
        put(m, "Ley", "Law");
        put(m, "Fuego", "Fire");
        put(m, "Tierra", "Earth");
        put(m, "Agua", "Water");
        put(m, "Luz", "Light");
        put(m, "Barrera", "Barrier");
        put(m, "Contra", "Against");
        put(m, "Bridas", "Reins");
        // Articles, prepositions, conjunctions.
        put(m, "el", "the");
        put(m, "la", "the");
        put(m, "los", "the");
        put(m, "las", "the");
        put(m, "un", "a");
        put(m, "una", "a");
        put(m, "unos", "some");
        put(m, "unas", "some");
        put(m, "de", "of");
        put(m, "del", "of the");
        put(m, "y", "and");
        put(m, "o", "or");
        put(m, "a", "to");
        put(m, "en", "in");
        put(m, "con", "with");
        put(m, "sin", "without");
        put(m, "para", "for");
        put(m, "por", "by");
        put(m, "no", "not");

        // Adjectives/nouns that show up across many category/skill/perk/spell names.
        put(m, "Ataque", "Attack");
        put(m, "Ataques", "Attacks");
        put(m, "Ataca", "Attack");
        put(m, "Defensa", "Defense");
        put(m, "Defensas", "Defenses");
        put(m, "Combate", "Combat");
        put(m, "Cuerpo", "Body");
        put(m, "Mano", "Hand");
        put(m, "Manos", "Hands");
        put(m, "Espada", "Sword");
        put(m, "Espadas", "Swords");
        put(m, "Corta", "Short");
        put(m, "Larga", "Long");
        put(m, "Daga", "Dagger");
        put(m, "Hacha", "Axe");
        put(m, "Maza", "Mace");
        put(m, "Lanza", "Spear");
        put(m, "Arco", "Bow");
        put(m, "Ballesta", "Crossbow");
        put(m, "Escudo", "Shield");
        put(m, "Yelmo", "Helm");
        put(m, "Cota", "Mail");
        put(m, "Mallas", "Mail");
        put(m, "Cuero", "Leather");
        put(m, "Coraza", "Breastplate");
        put(m, "Endurecido", "Hardened");
        put(m, "Blando", "Soft");
        put(m, "Poder", "Power");
        put(m, "Poderes", "Powers");
        put(m, "Chi", "Chi");
        put(m, "Golpe", "Strike");
        put(m, "Golpes", "Strikes");
        put(m, "Maniobras", "Maneuvers");
        put(m, "Estilo", "Style");
        put(m, "Adiestramiento", "Training");
        put(m, "Adiestramientos", "Trainings");
        put(m, "Adiestramiento Especial", "Special Training");
        put(m, "Habilidad", "Skill");
        put(m, "Habilidades", "Skills");
        put(m, "Categoría", "Category");
        put(m, "Categorías", "Categories");
        put(m, "Hechizo", "Spell");
        put(m, "Hechizos", "Spells");
        put(m, "Lista", "List");
        put(m, "Listas", "Lists");
        put(m, "Reino", "Realm");
        put(m, "Reinos", "Realms");
        put(m, "Magia", "Magic");
        put(m, "Mágica", "Magical");
        put(m, "Mágico", "Magical");
        put(m, "Mágicas", "Magical");
        put(m, "Mágicos", "Magical");
        put(m, "Rango", "Rank");
        put(m, "Rangos", "Ranks");
        put(m, "Punto", "Point");
        put(m, "Puntos", "Points");
        put(m, "Vida", "Life");
        put(m, "Muerte", "Death");
        put(m, "Trance", "Trance");
        put(m, "Sanador", "Healing");
        put(m, "Purificador", "Purifying");
        put(m, "Adormecedor", "Numbing");
        put(m, "Adrenal", "Adrenal");
        put(m, "Frenesí", "Frenzy");
        put(m, "Meditación", "Meditation");
        put(m, "Mnemotecnia", "Mnemonics");
        put(m, "Aturdimiento", "Stun");
        put(m, "Aturdido", "Stunned");
        put(m, "Salto", "Jump");
        put(m, "Saltar", "Jumping");
        put(m, "Velocidad", "Speed");
        put(m, "Fuerte", "Strong");
        put(m, "Equilibrio", "Balance");
        put(m, "Estabilización", "Stabilization");
        put(m, "Maniobrar", "Maneuvering");
        put(m, "Licantropía", "Lycanthropy");
        put(m, "Control", "Control");
        put(m, "Desenvainar", "Drawing Weapon");
        put(m, "Superar", "Overcoming");
        put(m, "Documentación", "Documentation");
        put(m, "Matemáticas", "Mathematics");
        put(m, "Avanzadas", "Advanced");
        put(m, "Alquimia", "Alchemy");
        put(m, "Antropología", "Anthropology");
        put(m, "Astronomía", "Astronomy");
        put(m, "Bioquímica", "Biochemistry");
        put(m, "Psicología", "Psychology");
        put(m, "Anatomía", "Anatomy");
        put(m, "Leer", "Reading");
        put(m, "Labios", "Lips");
        put(m, "Señales", "Signals");
        put(m, "Fauna", "Fauna");
        put(m, "Flora", "Flora");
        put(m, "Cultural", "Cultural");
        put(m, "Regional", "Regional");
        put(m, "Heráldica", "Heraldry");
        put(m, "Historia", "History");
        put(m, "Religión", "Religion");
        put(m, "Estilos", "Styles");
        put(m, "Artefactos", "Artifacts");
        put(m, "No-Muertos", "Undead");
        put(m, "Protecciones", "Protections");
        put(m, "Planos", "Planes");
        put(m, "Símbolos", "Symbols");
        put(m, "Hadas", "Fey");
        put(m, "Dragones", "Dragons");
        put(m, "Demonología", "Demonology");
        put(m, "Xeno-Conocimientos", "Xeno-Lore");
        put(m, "Piedra", "Stone");
        put(m, "Cerraduras", "Locks");
        put(m, "Hierbas", "Herbs");
        put(m, "Metales", "Metals");
        put(m, "Venenos", "Poisons");
        put(m, "Comercio", "Trade");
        put(m, "Actuar", "Acting");
        put(m, "Bailar", "Dancing");
        put(m, "Cantar", "Singing");
        put(m, "Imitar", "Imitating");
        put(m, "Sonidos", "Sounds");
        put(m, "Improvisación", "Improvisation");
        put(m, "Poética", "Poetic");
        put(m, "Mímica", "Mime");
        put(m, "Narrar", "Storytelling");
        put(m, "Historias", "Stories");
        put(m, "Tocar", "Playing");
        put(m, "Instrumento", "Instrument");
        put(m, "Ventriloquia", "Ventriloquism");
        put(m, "Escultura", "Sculpture");
        put(m, "Música", "Music");
        put(m, "Pintura", "Painting");
        put(m, "Poesía", "Poetry");
        put(m, "Juegos", "Games");
        put(m, "Atléticos", "Athletic");
        put(m, "Acrobacias", "Acrobatics");
        put(m, "Caer", "Falling");
        put(m, "Contorsionismo", "Contortion");
        put(m, "Malabarismos", "Juggling");
        put(m, "Trepar", "Climbing");
        put(m, "Volar", "Flying");
        put(m, "Planear", "Gliding");
        put(m, "Volteretas", "Tumbling");
        put(m, "Caminar", "Walking");
        put(m, "Zancos", "Stilts");
        put(m, "Cuerda", "Rope");
        put(m, "Floja", "Slack");
        put(m, "Esquiar", "Skiing");
        put(m, "Surf", "Surfing");
        put(m, "Patinar", "Skating");
        put(m, "Rappelling", "Rappelling");
        put(m, "Pértiga", "Pole");
        put(m, "Levantar", "Lifting");
        put(m, "Pesos", "Weights");
        put(m, "Poderoso", "Powerful");
        put(m, "Lanzamiento", "Throw");
        put(m, "Fondo", "Distance");
        put(m, "Escalar", "Climbing");
        put(m, "Esprintar", "Sprinting");
        put(m, "Nadar", "Swimming");
        put(m, "Remar", "Rowing");
        put(m, "Documentar", "Documenting");
        put(m, "Rastrear", "Tracking");
        put(m, "Acechar", "Stalking");
        put(m, "Esconderse", "Hiding");
        put(m, "Emboscar", "Ambushing");
        put(m, "Disfrazarse", "Disguising");
        put(m, "Callejeo", "Street Smarts");
        put(m, "Detectar", "Detecting");
        put(m, "Mentiras", "Lies");
        put(m, "Seducción", "Seduction");
        put(m, "Besos", "Kisses");
        put(m, "Engaños", "Deceptions");
        put(m, "Houri", "Houri");
        put(m, "Suerte", "Luck");
        put(m, "Dramática", "Dramatic");
        put(m, "Aspecto", "Aspect");
        put(m, "Único", "Unique");
        put(m, "Bonificación", "Bonus");
        put(m, "Fuerza Adrenal", "Adrenal Strength");
        put(m, "Vampiro", "Vampire");
        put(m, "Racial", "Racial");
    }
}
