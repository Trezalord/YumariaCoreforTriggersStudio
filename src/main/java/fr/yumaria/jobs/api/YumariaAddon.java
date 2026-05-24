package fr.yumaria.jobs.api;

import java.util.List;

public interface YumariaAddon {
    String id();

    String displayName();

    String version();

    List<String> providedProfessions();
}
