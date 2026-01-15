package com.example.addressmatch.service;

import com.example.addressmatch.entity.TableA;
import com.example.addressmatch.model.AddressTreeNode;
import com.example.addressmatch.model.MatchCandidate;
import com.example.addressmatch.util.CommonUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Service
public class MultiGranularityMatcher {

    @Autowired
    private AddressParserService addressParser;

    public List<MatchCandidate> matchAddress(String addressB, AddressTreeNode root) {
        List<String> componentsB = addressParser.parseAddressComponents(addressB);
        List<MatchCandidate> finalCandidates = new ArrayList<>();

        log.debug("开始多粒度匹配，地址: {}", addressB);

        for (int granularity = 1; granularity <= componentsB.size(); granularity++) {
            log.debug("正在第{}级粒度匹配...", granularity);

            List<MatchCandidate> levelCandidates = matchAtGranularity(
                    componentsB.subList(0, granularity), root);

            if (levelCandidates.isEmpty()) {
                levelCandidates = relaxMatchConditions(componentsB.subList(0, granularity), root);
            }

            if (!levelCandidates.isEmpty()) {
                levelCandidates = rankAndDeduplicate(levelCandidates);

                if (hasHighConfidenceMatch(levelCandidates)) {
                    log.debug("在第{}级找到高置信度匹配，提前终止", granularity);
                    return levelCandidates.subList(0, Math.min(5, levelCandidates.size()));
                }

                finalCandidates = levelCandidates;
            }
        }

        return finalCandidates;
    }

    private List<MatchCandidate> matchAtGranularity(List<String> components,
                                                    AddressTreeNode root) {
        List<MatchCandidate> candidates = new ArrayList<>();
        List<AddressTreeNode> matchingNodes = findMatchingPath(components, root);

        for (AddressTreeNode node : matchingNodes) {
            for (TableA address : node.getAddresses()) {
                double score = calculateMatchScore(components, address, node);
                candidates.add(new MatchCandidate(address, score));
            }
        }

        return candidates;
    }

    private List<AddressTreeNode> findMatchingPath(List<String> components,
                                                   AddressTreeNode root) {
        List<AddressTreeNode> allMatches = new ArrayList<>();
        findPathRecursive(components, 0, root, allMatches);
        return allMatches;
    }

    private void findPathRecursive(List<String> components, int index,
                                   AddressTreeNode currentNode,
                                   List<AddressTreeNode> matches) {
        if (index >= components.size()) {
            if (currentNode.isLeaf() && currentNode.hasAddresses()) {
                matches.add(currentNode);
            }
            return;
        }

        String targetComponent = components.get(index);

        AddressTreeNode exactMatch = currentNode.getChildren().get(targetComponent);
        if (exactMatch != null) {
            findPathRecursive(components, index + 1, exactMatch, matches);
        }

        for (Map.Entry<String, AddressTreeNode> entry : currentNode.getChildren().entrySet()) {
            if (CommonUtils.isFuzzyMatch(targetComponent, entry.getKey())) {
                findPathRecursive(components, index + 1, entry.getValue(), matches);
            }
        }

        if (targetComponent.isEmpty()) {
            for (AddressTreeNode child : currentNode.getChildren().values()) {
                findPathRecursive(components, index + 1, child, matches);
            }
        }
    }

    private List<MatchCandidate> relaxMatchConditions(List<String> components,
                                                      AddressTreeNode root) {
        return new ArrayList<>();
    }

    private double calculateMatchScore(List<String> componentsB,
                                       TableA addressA,
                                       AddressTreeNode matchedNode) {
        List<String> componentsA = addressParser.parseAddressComponents(addressA.getAddressA());

        int exactMatches = 0;
        int totalLevels = Math.max(componentsA.size(), componentsB.size());

        for (int i = 0; i < Math.min(componentsA.size(), componentsB.size()); i++) {
            if (componentsA.get(i).equals(componentsB.get(i))) {
                exactMatches++;
            } else if (CommonUtils.isFuzzyMatch(componentsA.get(i), componentsB.get(i))) {
                exactMatches += 0.8;
            }
        }

        double baseScore = (double) exactMatches / totalLevels;
        double depthScore = calculateDepthScore(matchedNode);

        return Math.min(1.0, baseScore * 0.9 + depthScore * 0.1);
    }

    private double calculateDepthScore(AddressTreeNode node) {
        int depth = node.getDepth();
        return Math.min(1.0, depth / 8.0);
    }

    private List<MatchCandidate> rankAndDeduplicate(List<MatchCandidate> candidates) {
        Set<MatchCandidate> uniqueCandidates = new LinkedHashSet<>(candidates);
        List<MatchCandidate> sorted = new ArrayList<>(uniqueCandidates);
        sorted.sort(Collections.reverseOrder());
        return sorted;
    }

    private boolean hasHighConfidenceMatch(List<MatchCandidate> candidates) {
        return !candidates.isEmpty() && candidates.get(0).getScore() > 0.95;
    }
}