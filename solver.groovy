import groovy.transform.Field
import groovyjarjarantlr.collections.List
import org.codehaus.groovy.util.ListHashMap
import org.testng.collections.ListMultiMap

class Node {
    int root;
    Node left, right;

    Node() {
        left = right = null
    }
}

@Field def clauses = []
@Field def numberOfVars
@Field def numberOfClauses
@Field def assignment = []
@Field ListMultiMap<Integer, List> map = ListMultiMap.create()
@Field ListHashMap<Integer, Integer> mapSize = new LinkedHashMap<Integer, Integer>()

def getData() {
    def cnfFile = new File(args[0])
    def lines = cnfFile.readLines()
    for (String l : lines) {
        l = l.trim()
        if (!l.startsWith("c")) {
            if (l.startsWith("p")) {
                s = l.split(" ")
                numberOfVars = Integer.parseInt(s[2])
                numberOfClauses = Integer.parseInt(s[3])
                assignment = new Integer[numberOfVars + 1]
                assignment[0] = 0
            } else {
                def s = l.split(" ")
                def singleClause = []
                def itr = s.iterator()
                singleClause = itr.collectMany { Integer.parseInt(it) == 0 ? [] : [Integer.parseInt(it)] }
                singleClause = singleClause.unique()
                for (int lit : singleClause) {
                    map.put(lit, singleClause)
                }
                clauses << singleClause
            }
        }
    }
}

getData()

println("c " + clauses)

def createBDD(ArrayList clause, int index) {
    if (index >= clause.size()) return null
    Node clauseBDD = new Node()
    clauseBDD.root = Math.abs(clause[index])
    if (clause[index] > 0) {
        addToLeft(clauseBDD, clause, index + 1)
    } else {
        addToRight(clauseBDD, clause, index + 1)
    }
    return clauseBDD
}

def addToRight(Node addTo, ArrayList clause, int index) {
    Node right = new Node()
    Node left = new Node()
    left.root = numberOfVars + 1
    Node enode = createBDD(clause, index)
    if (enode == null) {
        enode = new Node()
        enode.root = 0
    }
    addTo.right = enode
    addTo.left = left
}

def addToLeft(Node addTo, ArrayList clause, int index) {
    Node left = new Node()
    Node right = new Node()
    right.root = numberOfVars + 1
    Node enode = createBDD(clause, index)
    if (enode == null) {
        enode = new Node()
        enode.root = 0
    }
    addTo.left = enode
    addTo.right = right
}


def iterate() {
    Node returnedTree = new Node()
    for (int i = 0; i < numberOfClauses; i++) {
        Node bdd = createBDD(clauses[i], 0)
        if(i == 0) {
            returnedTree.root = numberOfVars+1
        }
        returnedTree = combine(bdd, returnedTree)
    }
    dfs(returnedTree,[])
}

def dfs(tree, ArrayList visited) {
    def flag
    if (tree != null) {
        if(tree.root == numberOfVars+1) {
            printSAT(visited)
            return true
        }
        visited << tree.root*-1
        flag = dfs(tree.left, visited)
        if(flag) return flag
        visited.remove(visited.size()-1)
        visited << tree.root
        flag = dfs(tree.right, visited)
        if(flag) return flag
        visited.remove(visited.size()-1)
//        isTerminal(tree)
    }
    if(visited.size() == 0 && !flag)
    println("s UNSATISFIABLE")
}

def isTerminal(node) {
    if (node.right == null && node.left == null) {
        println("YES " + node.root)
    }
}

iterate()

def combine(Node one, Node two) {
    Node returnTree = new Node()
    if (one.root == numberOfVars + 1) {
        return two
    } else if (two.root == numberOfVars + 1) {
        return one
    } else if (one.root == 0) {
        return one
    } else if (two.root == 0) {
        return two
    } else if (one.root == two.root) {
        returnTree.root = one.root
        returnTree.left = combine(one.left, two.left)
        returnTree.right = combine(one.right, two.right)
    } else if (one.root < two.root) {
        returnTree.root = one.root
        returnTree.left = combine(one.left, two)
        returnTree.right = combine(one.right, two)
    } else if (one.root > two.root) {
        returnTree.right = two.root
        returnTree.left = combine(one, two.left)
        returnTree.right = combine(one, two.right)
    }
    return returnTree
}

def printSAT(visited) {
    println("s SATISFIABLE")
    print("v ")
    for(int i : visited) {
        print(i + " ")
    }
    print("0")
}