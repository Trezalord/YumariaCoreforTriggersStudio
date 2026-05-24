package fr.yumaria.jobs.progression;

public final class EventXpModifier implements XpModifier {
    @Override
    public String id() {
        return "event";
    }

    @Override
    public XpModifierResult apply(XpModifierContext context) {
        return XpModifierResult.one("Bukkit YumariaJobXpGainEvent may adjust final XP after pipeline");
    }
}
