package com.udacity.webcrawler;

import com.udacity.webcrawler.parser.PageParser;
import com.udacity.webcrawler.parser.PageParserFactory;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.RecursiveAction;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/* A RecursiveAction class implemented using Build Pattern */
public final class CrawlTask extends RecursiveAction {

    private final Clock clock;
    private final Instant deadline;
    private final PageParserFactory parserFactory;
    private final int maxDepth;
    private final List<Pattern> ignoredUrls;
    private final String url;
    private final ConcurrentHashMap<String, Integer> counts;
    private final ConcurrentSkipListSet<String> visitedUrls;

    private CrawlTask (Clock clock, Instant deadline, PageParserFactory parserFactory, int maxDepth, List<Pattern> ignoredUrls, String url, ConcurrentHashMap<String, Integer> counts, ConcurrentSkipListSet<String> visitedUrls) {
        this.clock = clock;
        this.deadline = deadline;
        this.parserFactory = parserFactory;
        this.maxDepth = maxDepth;
        this.ignoredUrls = ignoredUrls;
        this.url = url;
        this.counts = counts;
        this.visitedUrls = visitedUrls;
    }

    @Override
    protected void compute() {
        if (maxDepth == 0 || clock.instant().isAfter(deadline)) {
            return;
        }
        for (Pattern pattern : ignoredUrls) {
            if (pattern.matcher(url).matches()) {
                return;
            }
        }
        if (visitedUrls.contains(url)) {
            return;
        }
        // update the visited urls set
        visitedUrls.add(url);
        PageParser.Result result = parserFactory.get(url).parse();

        // update the wordCounts map using atomic operation
        for (Map.Entry<String, Integer> e : result.getWordCounts().entrySet()) {
            counts.compute(e.getKey(), (k, v) -> v == null ? e.getValue() : e.getValue() + counts.get(e.getKey()));
        }

        // build subtasks and invoke all of them
        List<CrawlTask> subTasks = result.getLinks()
                .stream()
                .map(link -> new CrawlTask.Builder()
                        .setClock(clock)
                        .setCounts(counts)
                        .setDeadline(deadline)
                        .setIgnoredUrls(ignoredUrls)
                        .setMaxDepth(maxDepth - 1)
                        .setParserFactory(parserFactory)
                        .setUrl(link)
                        .setVisitedUrls(visitedUrls)
                        .build()
                ).collect(Collectors.toList());
        invokeAll(subTasks);
    }

    /* Below are getter methods for CrawlTask */
    public Clock getClock() {
        return clock;
    }

    public Instant getDeadline() {
        return deadline;
    }

    public PageParserFactory getParserFactory() {
        return parserFactory;
    }

    public int getMaxDepth() {
        return maxDepth;
    }

    public List<Pattern> getIgnoredUrls() {
        return ignoredUrls;
    }

    public String getUrl() {
        return url;
    }

    public ConcurrentHashMap<String, Integer> getCounts() {
        return counts;
    }

    public ConcurrentSkipListSet<String> getVisitedUrls() {
        return visitedUrls;
    }

    /* builder class for CrawlTask */
    public static final class Builder {
        private Clock clock;
        private Instant deadline;
        private PageParserFactory parserFactory;
        private int maxDepth;
        private List<Pattern> ignoredUrls;
        private String url;
        private ConcurrentHashMap<String, Integer> counts;
        private ConcurrentSkipListSet<String> visitedUrls;

        public CrawlTask build() {
            return new CrawlTask(
                    clock,
                    deadline,
                    parserFactory,
                    maxDepth,
                    ignoredUrls,
                    url,
                    counts,
                    visitedUrls);
        }

        public Builder setClock(Clock clock) {
            this.clock = clock;
            return this;
        }

        public Builder setDeadline(Instant deadline) {
            this.deadline = deadline;
            return this;
        }

        public Builder setParserFactory(PageParserFactory parserFactory) {
            this.parserFactory = parserFactory;
            return this;
        }

        public Builder setMaxDepth(int maxDepth) {
            this.maxDepth = maxDepth;
            return this;
        }

        public Builder setIgnoredUrls(List<Pattern> ignoredUrls) {
            this.ignoredUrls = ignoredUrls;
            return this;
        }

        public Builder setUrl(String url) {
            this.url = url;
            return this;
        }

        public Builder setCounts(ConcurrentHashMap<String, Integer> counts) {
            this.counts = counts;
            return this;
        }

        public Builder setVisitedUrls(ConcurrentSkipListSet<String> visitedUrls) {
            this.visitedUrls = visitedUrls;
            return this;
        }
    }
}
