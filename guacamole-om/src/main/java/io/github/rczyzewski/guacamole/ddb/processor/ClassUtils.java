package io.github.rczyzewski.guacamole.ddb.processor;

import io.github.rczyzewski.guacamole.ddb.processor.model.ClassDescription;
import io.github.rczyzewski.guacamole.ddb.processor.model.FieldDescription;
import io.github.rczyzewski.guacamole.ddb.processor.model.IndexDescription;
import lombok.AllArgsConstructor;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;


@AllArgsConstructor
public class ClassUtils
{

    private final ClassDescription classDescription;
    private Logger logger;

    public FieldDescription getPrimaryHash()
    {

        return classDescription.getFieldDescriptions()
            .stream()
            .filter(FieldDescription::isHashKey)
            .findFirst()
            .orElseThrow(() -> {
                String errorMessage = String.format("there is no HashKey defined for %s %s",
                                                    classDescription.getPackageName(), classDescription.getName());
                logger.warn(errorMessage);
                return new RuntimeException(errorMessage);
            });

    }

    public Set<FieldDescription> getAttributes()
    {

        return classDescription.getFieldDescriptions()
            .stream()
            .filter(it -> it.isHashKey() ||
                          it.isRangeKey() ||
                          null != it.getLocalIndex() ||
                          !it.getGlobalIndexHash().isEmpty() ||
                          !it.getGlobalIndexRange().isEmpty()
            )
            .collect(Collectors.toSet());

    }

    public Optional<FieldDescription> getPrimaryRange()
    {

        return classDescription.getFieldDescriptions()
            .stream()
            .filter(FieldDescription::isRangeKey)
            .findFirst();
    }

    public List<FieldDescription> getLSIndexes()
    {
        return classDescription.getFieldDescriptions()
            .stream()
            .filter(it -> null != it.getLocalIndex())
            .collect(Collectors.toList());
    }

    public List<String> getGSIndexHash()
    {
        return classDescription.getFieldDescriptions()
            .stream()
            .flatMap(it -> it.getGlobalIndexHash().stream())
            .collect(Collectors.toList());
    }

    public Optional<FieldDescription> getGSIndexHash(String indexName)
    {

        return classDescription.getFieldDescriptions()
            .stream()
            .filter(it -> it.getGlobalIndexHash().contains(indexName))
            .findFirst();
    }

    public Optional<FieldDescription> getGSIndexRange(String indexName)
    {

        return classDescription.getFieldDescriptions()
            .stream()
            .filter(it -> it.getGlobalIndexRange().contains(indexName))
            .findFirst();
    }

    public List<IndexDescription> createIndexsDescription()
    {

        IndexDescription primary = IndexDescription.builder()
            .hashField(getPrimaryHash())
            .rangeField(getPrimaryRange().orElse(null))
            .attributes(classDescription.getFieldDescriptions()
                            .stream()
                            .filter(it -> !(it.isHashKey() || it.isRangeKey()))
                            .collect(Collectors.toList())
            )

            .build();
        logger.info("PRIMARY:   " + primary);

        List<IndexDescription> localIndexes = getLSIndexes()
            .stream()
            .map(fieldDescription -> IndexDescription.builder()
                .name(fieldDescription.getLocalIndex())
                .hashField(getPrimaryHash())
                .rangeField(classDescription.getFieldDescriptions()
                                .stream()
                                .filter(d -> fieldDescription.getLocalIndex().equals(d.getLocalIndex()))
                                .findAny().orElse(null))
                .attributes(classDescription.getFieldDescriptions()
                                .stream()
                                .filter(a -> !fieldDescription.getLocalIndex().equals(a.getLocalIndex()))
                                .collect(Collectors.toList()))
                .build()
            )
            .collect(Collectors.toList());

        List<IndexDescription> globalIndexes = getGSIndexHash()
            .stream()
            .map(indexName -> IndexDescription.builder()
                .name(indexName)
                .hashField(getGSIndexHash(indexName).orElse(null))
                .rangeField(classDescription.getFieldDescriptions()
                                .stream()
                                .filter(d -> d.getGlobalIndexRange().contains(indexName))
                                .findAny().orElse(null))
                .attributes(classDescription.getFieldDescriptions()
                                .stream()
                                .filter(a -> !a.getGlobalIndexHash().contains(indexName))
                                .collect(Collectors.toList()))
                .build()
            )
            .collect(Collectors.toList());

        return Stream.of(Collections.singletonList(primary), localIndexes, globalIndexes)
            .flatMap(Collection::stream)
            .collect(Collectors.toList());
    }

}
