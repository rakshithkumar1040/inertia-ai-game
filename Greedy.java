import java.util.*;

public class Greedy {

    private static final int HARD_DEPTH = 4;

    public static Direction choose(BoardModel m, Difficulty level) {
        RegionPolicy policy = computeRegionsDivideConquer(m);
        
        switch (level) {
            case EASY:   return playEasyDCDirected(m, policy);
            case MEDIUM: return playMediumDCDirected(m, policy);
            case HARD:   return playHardDCDirected(m, policy);
            default:     return playMediumDCDirected(m, policy);
        }
    }

    /******************************************************************
     * FUNCTION 4: TRUE DIVIDE AND CONQUER - Global Board Partitioning
     * Classic D&C: Recursively divides board, analyzes, combines results
     ******************************************************************/
    private static RegionPolicy computeRegionsDivideConquer(BoardModel m) {
        // Create root region covering entire board
        Region root = new Region(0, m.rows, 0, m.cols, 0);
        
        // TRUE D&C: Recursively divide and analyze
        root = divideAndConquerAnalyze(m, root, 3); // Depth 3 = up to 64 subregions
        
        // Find best and worst regions using D&C search
        Region best = findExtremeRegion(root, true);  // true = find max
        Region worst = findExtremeRegion(root, false); // false = find min
        
        return new RegionPolicy(best, worst);
    }
    
    // TRUE D&C METHOD: Recursively divides region
    private static Region divideAndConquerAnalyze(BoardModel m, Region region, int depth) {
        // BASE CASE: Region is small enough or depth limit reached
        if (depth == 0 || region.area() <= 4) {
            analyzeRegionDirectly(m, region);
            return region;
        }
        
        // DIVIDE: Split into 4 equal quadrants (classic D&C division)
        int midRow = (region.rowStart + region.rowEnd) / 2;
        int midCol = (region.colStart + region.colEnd) / 2;
        
        // Recursively process each quadrant (CONQUER step)
        Region[] quadrants = new Region[4];
        quadrants[0] = divideAndConquerAnalyze(m, 
            new Region(region.rowStart, midRow, region.colStart, midCol, region.index * 4 + 1), 
            depth - 1);
        quadrants[1] = divideAndConquerAnalyze(m, 
            new Region(region.rowStart, midRow, midCol, region.colEnd, region.index * 4 + 2), 
            depth - 1);
        quadrants[2] = divideAndConquerAnalyze(m, 
            new Region(midRow, region.rowEnd, region.colStart, midCol, region.index * 4 + 3), 
            depth - 1);
        quadrants[3] = divideAndConquerAnalyze(m, 
            new Region(midRow, region.rowEnd, midCol, region.colEnd, region.index * 4 + 4), 
            depth - 1);
        
        // COMBINE: Aggregate results from quadrants
        region.gemCount = 0;
        region.shieldCount = 0;
        region.mineCount = 0;
        region.wallCount = 0;
        region.deadEndCount = 0;
        
        for (Region quad : quadrants) {
            region.gemCount += quad.gemCount;
            region.shieldCount += quad.shieldCount;
            region.mineCount += quad.mineCount;
            region.wallCount += quad.wallCount;
            region.deadEndCount += quad.deadEndCount;
            region.subregions.add(quad);
        }
        
        // Calculate combined score
        region.score = region.computeScore();
        
        return region;
    }
    
    // TRUE D&C METHOD: Find extreme region (max or min score)
    private static Region findExtremeRegion(Region region, boolean findMax) {
        // BASE CASE: Leaf region (no subregions)
        if (region.subregions.isEmpty()) {
            return region;
        }
        
        // DIVIDE: Find extreme in each subregion
        List<Region> extremeSubregions = new ArrayList<>();
        for (Region sub : region.subregions) {
            extremeSubregions.add(findExtremeRegion(sub, findMax));
        }
        
        // COMBINE: Select the most extreme from subregions
        Region extreme = extremeSubregions.get(0);
        for (int i = 1; i < extremeSubregions.size(); i++) {
            Region current = extremeSubregions.get(i);
            if (findMax) {
                if (current.score > extreme.score) {
                    extreme = current;
                }
            } else {
                if (current.score < extreme.score) {
                    extreme = current;
                }
            }
        }
        
        return extreme;
    }

    /******************************************************************
     * FUNCTION 1: TRUE DIVIDE AND CONQUER - Action Space Evaluation
     * Classic D&C: Recursively divides direction set, evaluates, combines
     ******************************************************************/
    private static Direction playEasyDCDirected(BoardModel m, RegionPolicy policy) {
        // Get all possible directions
        Direction[] allDirections = Direction.values();
        
        // Convert to list for D&C processing
        List<Direction> directions = new ArrayList<>(Arrays.asList(allDirections));
        
        // TRUE D&C: Recursively evaluate directions
        DirectionResult result = divideAndConquerEvaluateDirections(m, policy, directions, 0);
        
        return result != null ? result.direction : Direction.N; // Default fallback
    }
    
    // TRUE D&C METHOD: Recursively evaluates direction set
    private static DirectionResult divideAndConquerEvaluateDirections(BoardModel m, RegionPolicy policy, 
                                                                     List<Direction> directions, int depth) {
        // BASE CASE: 0 or 1 direction
        if (directions.isEmpty()) {
            return null;
        }
        if (directions.size() == 1) {
            Direction d = directions.get(0);
            return new DirectionResult(d, evaluateSingleDirection(m, policy, d));
        }
        if (directions.size() == 2) {
            // Direct comparison for small set
            Direction d1 = directions.get(0);
            Direction d2 = directions.get(1);
            double score1 = evaluateSingleDirection(m, policy, d1);
            double score2 = evaluateSingleDirection(m, policy, d2);
            return score1 >= score2 ? 
                new DirectionResult(d1, score1) : 
                new DirectionResult(d2, score2);
        }
        
        // DIVIDE: Split into two halves (classic D&C binary split)
        int mid = directions.size() / 2;
        List<Direction> leftHalf = new ArrayList<>(directions.subList(0, mid));
        List<Direction> rightHalf = new ArrayList<>(directions.subList(mid, directions.size()));
        
        // CONQUER: Recursively evaluate each half
        DirectionResult leftBest = divideAndConquerEvaluateDirections(m, policy, leftHalf, depth + 1);
        DirectionResult rightBest = divideAndConquerEvaluateDirections(m, policy, rightHalf, depth + 1);
        
        // COMBINE: Select best from halves
        if (leftBest == null) return rightBest;
        if (rightBest == null) return leftBest;
        
        return leftBest.score >= rightBest.score ? leftBest : rightBest;
    }

    /******************************************************************
     * FUNCTION 2: TRUE DIVIDE AND CONQUER - Search Space Exploration
     * Classic D&C: Recursively divides search space, explores, combines paths
     ******************************************************************/
    private static Direction playMediumDCDirected(BoardModel m, RegionPolicy policy) {
        // TRUE D&C: Recursively search for gems
        SearchResult result = divideAndConquerSearchForGem(m, policy, 
            m.cpuRow, m.cpuCol, m.cpuShields, 
            new HashSet<>(), 0, 4); // Depth 4 search
        
        return result != null ? result.firstDirection : 
               divideAndConquerEvaluateDirections(m, policy, 
                   new ArrayList<>(Arrays.asList(Directio       n.values())), 0).direction;
    }
    
    // TRUE D&C METHOD: Recursively searches for gems
    private static SearchResult divideAndConquerSearchForGem(BoardModel m, RegionPolicy policy,
                                                            int r, int c, int shields,
                                                            Set<String> visited, int depth, int maxDepth) {
        // BASE CASE: Depth limit reached
        if (depth >= maxDepth) {
            return null;
        }
        
        // Get all possible moves from current position
        List<Direction> possibleDirections = new ArrayList<>();
        for (Direction d : Direction.values()) {
            BoardModel.SlideResult slideRes = m.slide(r, c, d, false);
            if (!isDeadlyMove(slideRes, shields) && (slideRes.r != r || slideRes.c != c)) {
                possibleDirections.add(d);
            }
        }
        
        // BASE CASE: No possible moves
        if (possibleDirections.isEmpty()) {
            return null;
        }
        
        // DIVIDE: Split search space into regions based on direction
        Map<String, List<Direction>> directionGroups = new HashMap<>();
        for (Direction d : possibleDirections) {
            String group = getDirectionGroup(d);
            directionGroups.computeIfAbsent(group, k -> new ArrayList<>()).add(d);
        }
        
        List<SearchResult> groupResults = new ArrayList<>();
        
        // CONQUER: Search each group independently
        for (List<Direction> group : directionGroups.values()) {
            // Further divide group if large
            if (group.size() > 2) {
                int mid = group.size() / 2;
                List<Direction> subgroup1 = group.subList(0, mid);
                List<Direction> subgroup2 = group.subList(mid, group.size());
                
                SearchResult res1 = searchDirectionSubgroup(m, policy, r, c, shields, 
                                                           visited, depth, maxDepth, subgroup1);
                SearchResult res2 = searchDirectionSubgroup(m, policy, r, c, shields, 
                                                           visited, depth, maxDepth, subgroup2);
                
                if (res1 != null) groupResults.add(res1);
                if (res2 != null) groupResults.add(res2);
            } else {
                SearchResult res = searchDirectionSubgroup(m, policy, r, c, shields, 
                                                          visited, depth, maxDepth, group);
                if (res != null) groupResults.add(res);
            }
        }
        
        // COMBINE: Select best result from all groups
        if (groupResults.isEmpty()) {
            return null;
        }
        
        SearchResult bestResult = groupResults.get(0);
        for (int i = 1; i < groupResults.size(); i++) {
            if (compareSearchResults(groupResults.get(i), bestResult) > 0) {
                bestResult = groupResults.get(i);
            }
        }
        
        return bestResult;
    }
    
    // Helper for D&C search
    private static SearchResult searchDirectionSubgroup(BoardModel m, RegionPolicy policy,
                                                       int r, int c, int shields,
                                                       Set<String> visited, int depth, int maxDepth,
                                                       List<Direction> directions) {
        SearchResult bestSubgroupResult = null;
        
        for (Direction d : directions) {
            BoardModel.SlideResult slideRes = m.slide(r, c, d, false);
            
            // Check for immediate gem
            if (slideRes.gems > 0) {
                SearchResult immediate = new SearchResult(d, slideRes.gems, 1);
                immediate.regionBonus = getRegionBonus(m, policy, slideRes.r, slideRes.c);
                if (bestSubgroupResult == null || 
                    compareSearchResults(immediate, bestSubgroupResult) > 0) {
                    bestSubgroupResult = immediate;
                }
                continue;
            }
            
            // Recursive search if move is valid
            String key = slideRes.r + "," + slideRes.c + "," + shields;
            if (!visited.contains(key)) {
                int nextShields = adjustShields(slideRes, shields);
                Set<String> newVisited = new HashSet<>(visited);
                newVisited.add(key);
                
                SearchResult deeperResult = divideAndConquerSearchForGem(m, policy, 
                    slideRes.r, slideRes.c, nextShields, 
                    newVisited, depth + 1, maxDepth);
                
                if (deeperResult != null) {
                    deeperResult.firstDirection = d;
                    deeperResult.depth += 1;
                    
                    if (bestSubgroupResult == null || 
                        compareSearchResults(deeperResult, bestSubgroupResult) > 0) {
                        bestSubgroupResult = deeperResult;
                    }
                }
            }
        }
        
        return bestSubgroupResult;
    }

    /******************************************************************
     * FUNCTION 3: TRUE DIVIDE AND CONQUER - Decision Tree Evaluation
     * Classic D&C: Recursively builds and evaluates decision tree
     ******************************************************************/
    private static Direction playHardDCDirected(BoardModel m, RegionPolicy policy) {
        // Build decision tree using D&C
        DecisionTree tree = buildDecisionTreeDAC(m, policy, 
            m.cpuRow, m.cpuCol, m.cpuShields, 
            new HashSet<>(), HARD_DEPTH);
        
        // Evaluate tree using D&C
        TreeEvaluation eval = evaluateTreeDAC(tree);
        
        return eval != null ? eval.bestDirection : 
               divideAndConquerEvaluateDirections(m, policy, 
                   new ArrayList<>(Arrays.asList(Direction.values())), 0).direction;
    }
    
    // TRUE D&C METHOD: Builds decision tree recursively
    private static DecisionTree buildDecisionTreeDAC(BoardModel m, RegionPolicy policy,
                                                    int r, int c, int shields,
                                                    Set<Integer> visited, int depth) {
        DecisionTree tree = new DecisionTree(r, c, shields);
        
        // BASE CASE: Depth limit or no valid moves
        if (depth == 0) {
            tree.score = evaluatePosition(m, policy, r, c);
            return tree;
        }
        
        // Get valid moves
        List<Direction> validMoves = getValidMoves(m, r, c, shields);
        if (validMoves.isEmpty()) {
            tree.score = evaluatePosition(m, policy, r, c);
            return tree;
        }
        
        // DIVIDE: Split moves into groups for parallel processing
        if (validMoves.size() > 2) {
            int groupSize = Math.max(2, validMoves.size() / 3);
            List<List<Direction>> moveGroups = new ArrayList<>();
            
            for (int i = 0; i < validMoves.size(); i += groupSize) {
                int end = Math.min(i + groupSize, validMoves.size());
                moveGroups.add(validMoves.subList(i, end));
            }
            
            // CONQUER: Build subtree for each group
            for (List<Direction> group : moveGroups) {
                List<DecisionTree> groupSubtrees = new ArrayList<>();
                
                for (Direction d : group) {
                    BoardModel.SlideResult res = m.slide(r, c, d, false);
                    int nextShields = adjustShields(res, shields);
                    
                    int cellKey = res.r * 1000 + res.c;
                    if (!visited.contains(cellKey)) {
                        Set<Integer> newVisited = new HashSet<>(visited);
                        newVisited.add(cellKey);
                        
                        DecisionTree subtree = buildDecisionTreeDAC(m, policy, 
                            res.r, res.c, nextShields, newVisited, depth - 1);
                        subtree.direction = d;
                        subtree.immediateValue = evaluateMove(m, policy, res, d);
                        groupSubtrees.add(subtree);
                    }
                }
                
                // COMBINE within group: Select best subtree
                if (!groupSubtrees.isEmpty()) {
                    DecisionTree bestInGroup = groupSubtrees.get(0);
                    for (int i = 1; i < groupSubtrees.size(); i++) {
                        if (groupSubtrees.get(i).getTotalValue() > bestInGroup.getTotalValue()) {
                            bestInGroup = groupSubtrees.get(i);
                        }
                    }
                    tree.children.add(bestInGroup);
                }
            }
        } else {
            // Small group, process directly
            for (Direction d : validMoves) {
                BoardModel.SlideResult res = m.slide(r, c, d, false);
                int nextShields = adjustShields(res, shields);
                
                int cellKey = res.r * 1000 + res.c;
                if (!visited.contains(cellKey)) {
                    Set<Integer> newVisited = new HashSet<>(visited);
                    newVisited.add(cellKey);
                    
                    DecisionTree subtree = buildDecisionTreeDAC(m, policy, 
                        res.r, res.c, nextShields, newVisited, depth - 1);
                    subtree.direction = d;
                    subtree.immediateValue = evaluateMove(m, policy, res, d);
                    tree.children.add(subtree);
                }
            }
        }
        
        return tree;
    }
    
    // TRUE D&C METHOD: Evaluates decision tree recursively
    private static TreeEvaluation evaluateTreeDAC(DecisionTree tree) {
        // BASE CASE: Leaf node
        if (tree.children.isEmpty()) {
            return new TreeEvaluation(tree.direction, tree.getTotalValue());
        }
        
        // DIVIDE: Evaluate each subtree
        List<TreeEvaluation> subtreeEvals = new ArrayList<>();
        for (DecisionTree child : tree.children) {
            subtreeEvals.add(evaluateTreeDAC(child));
        }
        
        // CONQUER: Combine subtree evaluations
        TreeEvaluation bestChildEval = subtreeEvals.get(0);
        double bestValue = bestChildEval.totalValue * 0.9; // Discount future
        
        for (int i = 1; i < subtreeEvals.size(); i++) {
            double childValue = subtreeEvals.get(i).totalValue * 0.9;
            if (childValue > bestValue) {
                bestValue = childValue;
                bestChildEval = subtreeEvals.get(i);
            }
        }
        
        // COMBINE: Current value + best future value
        double totalValue = tree.immediateValue + bestValue;
        Direction bestDir = (tree.direction != null) ? tree.direction : bestChildEval.bestDirection;
        
        return new TreeEvaluation(bestDir, totalValue);
    }

    /******************************************************************
     * Helper Methods
     ******************************************************************/
    private static void analyzeRegionDirectly(BoardModel m, Region region) {
        for (int r = region.rowStart; r < region.rowEnd; r++) {
            for (int c = region.colStart; c < region.colEnd; c++) {
                Cell cell = m.grid[r][c];
                
                if (cell.wall) region.wallCount++;
                else if (cell.mine) region.mineCount++;
                else if (cell.gem) region.gemCount++;
                else if (cell.shield) region.shieldCount++;
                
                if (!cell.wall && isDeadEnd(m, r, c)) {
                    region.deadEndCount++;
                }
            }
        }
        region.score = region.computeScore();
    }
    
    private static boolean isDeadEnd(BoardModel m, int r, int c) {
        int blocked = 0;
        for (Direction d : Direction.values()) {
            int nr = r + d.dx;
            int nc = c + d.dy;
            if (!m.inBounds(nr, nc) || m.grid[nr][nc].wall || m.grid[nr][nc].mine) {
                blocked++;
            }
        }
        return blocked >= 6;
    }
    
    private static double evaluateSingleDirection(BoardModel m, RegionPolicy policy, Direction d) {
        BoardModel.SlideResult res = m.slide(m.cpuRow, m.cpuCol, d, false);
        
        if (res.hitMine && m.cpuShields == 0) return -1000;
        if (res.r == m.cpuRow && res.c == m.cpuCol) return -500;
        
        double score = res.gems * 100 + res.shields * 50;
        score += getRegionBonus(m, policy, res.r, res.c);
        
        return score;
    }
    
    private static boolean isDeadlyMove(BoardModel.SlideResult res, int shields) {
        return res.hitMine && (shields == 0);
    }
    
    private static int adjustShields(BoardModel.SlideResult res, int shields) {
        int nextShields = shields + res.shields;
        if (res.hitMine && nextShields > 0) {
            nextShields--;
        }
        return nextShields;
    }
    
    private static String getDirectionGroup(Direction d) {
        if (d.dx == -1) return "NORTH";
        if (d.dx == 1) return "SOUTH";
        if (d.dy == -1) return "WEST";
        if (d.dy == 1) return "EAST";
        return "DIAGONAL";
    }
    
    private static int getRegionBonus(BoardModel m, RegionPolicy policy, int r, int c) {
        int regionIdx = getRegionIndex(m, r, c);
        int bonus = 0;
        if (policy.bestRegion != null && regionIdx == policy.bestRegion.index) bonus += 30;
        if (policy.worstRegion != null && regionIdx == policy.worstRegion.index) bonus -= 40;
        return bonus;
    }
    
    private static int getRegionIndex(BoardModel m, int r, int c) {
        int midRow = m.rows / 2;
        int midCol = m.cols / 2;
        if (r < midRow) {
            return (c < midCol) ? 1 : 2;
        } else {
            return (c < midCol) ? 3 : 4;
        }
    }
    
    private static int compareSearchResults(SearchResult a, SearchResult b) {
        if (a.value != b.value) return Integer.compare(a.value, b.value);
        if (a.depth != b.depth) return Integer.compare(b.depth, a.depth);
        return Integer.compare(b.regionBonus, a.regionBonus);
    }
    
    private static List<Direction> getValidMoves(BoardModel m, int r, int c, int shields) {
        List<Direction> valid = new ArrayList<>();
        for (Direction d : Direction.values()) {
            BoardModel.SlideResult res = m.slide(r, c, d, false);
            if (!isDeadlyMove(res, shields) && (res.r != r || res.c != c)) {
                valid.add(d);
            }
        }
        return valid;
    }
    
    private static double evaluatePosition(BoardModel m, RegionPolicy policy, int r, int c) {
        double score = 0;
        // Check adjacent cells for items
        for (Direction d : Direction.values()) {
            int nr = r + d.dx;
            int nc = c + d.dy;
            if (m.inBounds(nr, nc)) {
                Cell cell = m.grid[nr][nc];
                if (cell.gem) score += 50;
                if (cell.shield) score += 25;
                if (cell.mine) score -= 75;
            }
        }
        score += getRegionBonus(m, policy, r, c);
        return score;
    }
    
    private static double evaluateMove(BoardModel m, RegionPolicy policy, BoardModel.SlideResult res, Direction d) {
        double score = res.gems * 100 + res.shields * 50;
        score += getRegionBonus(m, policy, res.r, res.c);
        return score;
    }

    /******************************************************************
     * Helper Classes
     ******************************************************************/
    private static class Region {
        int rowStart, rowEnd, colStart, colEnd, index;
        int gemCount, shieldCount, mineCount, wallCount, deadEndCount;
        double score;
        List<Region> subregions = new ArrayList<>();
        
        Region(int rs, int re, int cs, int ce, int idx) {
            rowStart = rs; rowEnd = re; colStart = cs; colEnd = ce; index = idx;
        }
        
        int area() { return (rowEnd - rowStart) * (colEnd - colStart); }
        
        double computeScore() {
            double score = gemCount * 10.0 + shieldCount * 5.0 - 
                          mineCount * 15.0 - deadEndCount * 8.0 - 
                          wallCount * 0.5;
            int area = area();
            return area > 0 ? score / Math.sqrt(area) : score;
        }
    }
    
    private static class RegionPolicy {
        Region bestRegion, worstRegion;
        RegionPolicy(Region best, Region worst) {
            this.bestRegion = best; this.worstRegion = worst;
        }
    }
    
    private static class DirectionResult {
        Direction direction;
        double score;
        DirectionResult(Direction d, double s) { direction = d; score = s; }
    }
    
    private static class SearchResult {
        Direction firstDirection;
        int value, depth, regionBonus;
        SearchResult(Direction d, int v, int dep) {
            firstDirection = d; value = v; depth = dep;
        }
    }
    
    private static class DecisionTree {
        int r, c, shields;
        Direction direction;
        double immediateValue, score;
        List<DecisionTree> children = new ArrayList<>();
        
        DecisionTree(int r, int c, int s) {
            this.r = r; this.c = c; this.shields = s;
        }
        
        double getTotalValue() {
            return immediateValue + score;
        }
    }
    
    private static class TreeEvaluation {
        Direction bestDirection;
        double totalValue;
        TreeEvaluation(Direction d, double v) {
            bestDirection = d; totalValue = v;
        }
    }
    
    // Legacy Node class for compatibility
    private static class Node {
        int r, c, shields;
        Direction firstDir;
        Node(int r, int c, int s, Direction d) { 
            this.r = r; this.c = c; this.shields = s; this.firstDir = d; 
        }
        String key() { return r + "," + c + "," + shields; }
    }
}