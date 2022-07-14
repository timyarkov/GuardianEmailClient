package model.items;

/**
 * Data on a Guardian Tag.
 * @param id ID of the tag.
 * @param type The type of tag.
 * @param webTitle Title of the tag.
 * @param webUrl Usual Web URL for the tag.
 * @param apiUrl API URL for the tag.
 */
public record GTag(
        String id,
        String type,
        String webTitle,
        String webUrl,
        String apiUrl
) {
    @Override
    public String toString() {
        return this.id;
    }
}
