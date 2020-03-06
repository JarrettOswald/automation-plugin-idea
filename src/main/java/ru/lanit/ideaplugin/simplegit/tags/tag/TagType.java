package ru.lanit.ideaplugin.simplegit.tags.tag;

public enum  TagType {
    JIRA_TAG(JiraTag.class),
    COMMON_TAG(CommonTag.class),
    FEATURE_TAG(FeatureTag.class);

    private Class<? extends AbstractTag> aClass;

    TagType(Class<? extends AbstractTag> aClass) {
        this.aClass = aClass;
    }

    public static TagType getTagTypeByClass(Class<? extends AbstractTag> aClass) {
        for(TagType type : values()) {
            if (type.aClass == aClass) {
                return type;
            }
        }
        return null;
    }

    public static TagType getTagTypeByTag(AbstractTag tag) {
        return getTagTypeByClass(tag.getClass());
    }
}
