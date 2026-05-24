package fr.yumaria.jobs.action;

import fr.yumaria.jobs.YumariaJobsPlugin;

import java.util.ArrayList;
import java.util.List;

public final class DefaultActionModifierPipelineFactory {
    private DefaultActionModifierPipelineFactory() {
    }

    public static ActionModifierPipeline create(YumariaJobsPlugin plugin) {
        GlobalActionModifier global = new GlobalActionModifier(plugin);
        ProfessionActionModifier profession = new ProfessionActionModifier(plugin);
        AddonActionModifier addon = new AddonActionModifier(plugin);
        List<ActionModifier> modifiers = new ArrayList<>();
        modifiers.add(new DelegatingActionModifier("addon-action-xp", addon::xp));
        modifiers.add(new DelegatingActionModifier("global-money", global::money));
        modifiers.add(new DelegatingActionModifier("profession-money", profession::money));
        modifiers.add(new DelegatingActionModifier("addon-action-money", addon::money));
        return new ActionModifierPipeline(modifiers);
    }

    private record DelegatingActionModifier(String id, java.util.function.Function<ActionModifierContext, ModifierResult> delegate) implements ActionModifier {
        @Override
        public ModifierResult apply(ActionModifierContext context) {
            return delegate.apply(context);
        }
    }
}
