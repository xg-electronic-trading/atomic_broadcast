package atomic_broadcast.utils;

import java.util.ArrayList;
import java.util.List;

public class CompositePollable implements Pollable, PollableBuilder {

    private final List<Pollable> pollables = new ArrayList<>(20);

    public void add(Pollable pollable) {
        pollables.add(pollable);
    }

    @Override
    public void poll() {
        for (int i = 0; i < pollables.size(); i++) {
            pollables.get(i).poll();
        }
    }

    public List<Pollable> getPollables() {
        return pollables;
    }
}
