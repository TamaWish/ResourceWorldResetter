package io.github.tamawish.rwr.teleport;

import java.util.List;

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
