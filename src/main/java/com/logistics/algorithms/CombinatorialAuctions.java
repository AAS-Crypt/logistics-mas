package com.logistics.algorithms;

import java.util.*;

public class CombinatorialAuctions {
    public static class Bid {
        private String bidderId;
        private Set<String> bundle; 
        private double amount;
        
        public Bid(String bidderId, Set<String> bundle, double amount) {
            this.bidderId = bidderId;
            this.bundle = new HashSet<>(bundle);
            this.amount = amount;
        }
        public String getBidderId() {
            return bidderId;
        }
        public Set<String> getBundle() {
            return bundle;
        }
        public double getAmount() {
            return amount;
        }
        public boolean conflictsWith(Bid other) {
            for (String resource : bundle) {
                if (other.bundle.contains(resource)) {
                    return true;
                }
            }
            return false;
        }
    }
     
    public static class AuctionResult {
        private Set<Bid> winningBids;
        private double totalRevenue;
        private Map<String, String> allocation; 
        
        public AuctionResult(Set<Bid> winningBids, double totalRevenue) {
            this.winningBids = winningBids;
            this.totalRevenue = totalRevenue;
            this.allocation = new HashMap<>();
            for (Bid bid : winningBids) {
                for (String resource : bid.getBundle()) {
                    allocation.put(resource, bid.getBidderId());
                }
            }
        }
        public Set<Bid> getWinningBids() {
            return winningBids;
        }
        public double getTotalRevenue() {
            return totalRevenue;
        }
        public Map<String, String> getAllocation() {
            return allocation;
        }
    }
     
    public static AuctionResult runAuction(List<Bid> bids, Set<String> availableResources) {
        List<Bid> sortedBids = new ArrayList<>(bids);
        sortedBids.sort((b1, b2) -> Double.compare(b2.getAmount(), b1.getAmount()));
        Set<Bid> winningBids = new HashSet<>();
        Set<String> allocatedResources = new HashSet<>();
        double totalRevenue = 0;
        for (Bid bid : sortedBids) {
            boolean canAllocate = true;
            for (String resource : bid.getBundle()) {
                if (allocatedResources.contains(resource)) {
                    canAllocate = false;
                    break;
                }
            }
            if (canAllocate && bid.getBundle().size() <= availableResources.size()) {
                winningBids.add(bid);
                allocatedResources.addAll(bid.getBundle());
                totalRevenue += bid.getAmount();
            }
        }
        return new AuctionResult(winningBids, totalRevenue);
    }
     
    public static AuctionResult runVCGAuction(List<Bid> bids, Set<String> availableResources) {
        AuctionResult optimalResult = runAuction(bids, availableResources);
        Map<String, Double> payments = new HashMap<>();
        for (Bid winningBid : optimalResult.getWinningBids()) {
            List<Bid> bidsWithout = new ArrayList<>(bids);
            bidsWithout.removeIf(b -> b.getBidderId().equals(winningBid.getBidderId()));
            AuctionResult withoutResult = runAuction(bidsWithout, availableResources);
            double othersWelfareWithout = withoutResult.getTotalRevenue();
            double othersWelfareWith = optimalResult.getTotalRevenue() - winningBid.getAmount();
            double payment = othersWelfareWithout - othersWelfareWith;
            payments.put(winningBid.getBidderId(), payment);
        }
        Set<Bid> vcgWinningBids = new HashSet<>();
        for (Bid bid : optimalResult.getWinningBids()) {
            double vcgPayment = payments.getOrDefault(bid.getBidderId(), bid.getAmount());
            vcgWinningBids.add(new Bid(bid.getBidderId(), bid.getBundle(), vcgPayment));
        }
        double totalVCGRevenue = vcgWinningBids.stream().mapToDouble(Bid::getAmount).sum();
        return new AuctionResult(vcgWinningBids, totalVCGRevenue);
    }
     
    public static List<Bid> generateRandomBids(int numBidders, Set<String> resources, int maxBundleSize) {
        List<Bid> bids = new ArrayList<>();
        Random rand = new Random();
        List<String> resourceList = new ArrayList<>(resources);
        for (int i = 0; i < numBidders; i++) {
            int bundleSize = rand.nextInt(maxBundleSize) + 1;
            Set<String> bundle = new HashSet<>();
            for (int j = 0; j < bundleSize && j < resourceList.size(); j++) {
                int idx = rand.nextInt(resourceList.size());
                bundle.add(resourceList.get(idx));
            }
            double amount = 100 + rand.nextDouble() * 900; 
            bids.add(new Bid("bidder" + (i + 1), bundle, amount));
        }
        return bids;
    }
     
    public static double calculateEfficiency(AuctionResult result, List<Bid> allBids) {
        double maxWelfare = 0;
        for (Bid bid : allBids) {
            maxWelfare = Math.max(maxWelfare, bid.getAmount());
        }
        double actualWelfare = result.getTotalRevenue();
        return maxWelfare > 0 ? actualWelfare / maxWelfare : 0;
    }
}