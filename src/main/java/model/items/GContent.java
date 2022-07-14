package model.items;

/**
 * Data for content from the Guardian.
 * @param id ID of the content.
 * @param sectionId ID of the section this content belongs to.
 * @param sectionName Name of the section this content belongs to.
 * @param webPublicationDate When content was published.
 * @param webTitle Title of the content.
 * @param webUrl Usual Web URL for content.
 * @param apiUrl API URL for content.
 * @param pageNum Page number this content was gained in.
 * @param totalPages Total number of pages this content was part of.
 */
public record GContent(
        String id,
        String sectionId,
        String sectionName,
        String webPublicationDate,
        String webTitle,
        String webUrl,
        String apiUrl,
        int pageNum,
        int totalPages
) {
    @Override
    public String toString() {
        return "%s - %s (published %s)".formatted(webTitle(), sectionName(), webPublicationDate());
    }
}
