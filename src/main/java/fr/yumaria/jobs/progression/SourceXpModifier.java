package fr.yumaria.jobs.progression;

import fr.yumaria.jobs.job.JobSourceDefinition;
import fr.yumaria.jobs.util.Text;

public final class SourceXpModifier implements XpModifier {
    @Override
    public String id() {
        return "source";
    }

    @Override
    public XpModifierResult apply(XpModifierContext context) {
        JobSourceDefinition source = context.job().sources().get(Text.normalizeId(context.source()));
        if (source == null) {
            return XpModifierResult.one("source not configured");
        }
        return new XpModifierResult(source.multiplier(), "source " + context.source());
    }
}
