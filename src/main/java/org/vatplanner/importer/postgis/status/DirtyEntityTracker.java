package org.vatplanner.importer.postgis.status;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.vatplanner.importer.postgis.status.entities.DirtyMark;

public class DirtyEntityTracker {

    private final Map<Class<? extends DirtyMark>, Set<DirtyMark>> dirtyEntitiesByClass = new HashMap<>();

    public <T extends DirtyMark> void recordAsDirty(Class<T> entityClass, T entity) {
        Set<DirtyMark> dirtyEntities = dirtyEntitiesByClass.computeIfAbsent(entityClass, c -> new HashSet<>());
        dirtyEntities.add(entity);
    }

    public <T extends DirtyMark> void recordAsClean(Class<T> entityClass, T entity) {
        Set<DirtyMark> dirtyEntities = dirtyEntitiesByClass.computeIfAbsent(entityClass, c -> new HashSet<>());
        dirtyEntities.remove(entity);
    }

    public <T extends DirtyMark> Set<T> getDirtyEntities(Class<T> entityClass) {
        Set<T> dirtyEntities = (Set<T>) dirtyEntitiesByClass.computeIfAbsent(entityClass, c -> new HashSet<>());
        return Collections.unmodifiableSet(new HashSet<>(dirtyEntities));
    }

    public int countDirtyEntities() {
        return dirtyEntitiesByClass
                .values()
                .stream()
                .mapToInt(Set::size)
                .sum();
    }
}
