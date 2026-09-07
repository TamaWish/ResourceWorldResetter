package io.github.tamawish.rwr.teleport;

import java.util.List;

/**
 * One bounded page of teleport destinations.
 *
 * @param destinations destinations on this page
 * @param page zero-based page index
 * @param pageCount total number of pages
 */
public record TeleportPage(List<TeleportDestinationView> destinations, int page, int pageCount) {
  public TeleportPage {
    destinations = List.copyOf(destinations);
  }

  public boolean hasPrevious() {
    return page > 0;
  }

  public boolean hasNext() {
    return page + 1 < pageCount;
  }
}
