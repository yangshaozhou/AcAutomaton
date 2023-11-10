package com.dmwsy.platform.common;

import java.io.Serializable;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Queue;

public class ACAutomaton implements Serializable {

    private static final long serialVersionUID = 5912122461482105550L;

    public static class Match {
        public int end;
        public int length;
        public Info info;

        public Match(int end_, int length_, Info info_) {
            this.end = end_;
            this.length = length_;
            this.info = info_;
        }

        public String toString() {
            return String.format("%s:%d,%d", this.info.toString(), this.end - this.length + 1, this.end);
        }
    }

    protected ACNode root;
    protected Map core = new HashMap<Long, ACNode>();

    public ACAutomaton() {
        this.root = new ACNode();
    }

//    添加敏感词
    public void addCore(Info fruit_) {
        this.core.put(fruit_.getId(), fruit_);
    }

    public void delCore(Info fruit_) {
        this.core.remove(fruit_.getId());
    }

    public boolean checkCore(Info fruit_) {
        return this.core.containsKey(fruit_.getId());
    }

    public boolean checkCore(long id) {
        return this.core.containsKey(id);
    }

//    建立tire树
    public void addBranch(Info fruit_) {
//        getString方法是将字符串切割为每个只有一个字符保存的字符串数组
        String[] strarray = fruit_.getString();
        for (String str : strarray) {
            String[] strs = str.split(" ");
            ACNode currentnode = this.root;
            for (int i = 0; i < strs.length; ++i) {
//                添加分支
                currentnode = currentnode.addBranch(strs[i]);
            }
//            添加每个句子最后的果实，即结束的完整句子
            currentnode.addFruit(fruit_);
        }

        // strarray = fruit_.getPinYin(); --添加拼音过滤
        for (String str : strarray) {
            str = str.replaceAll(" ", "");
            str = StringMachine.insertBlank(str);
            String[] strs = str.split(" ");
            ACNode currentnode = this.root;
            for (int i = 0; i < strs.length; ++i) {
                currentnode = currentnode.addBranch(strs[i]);
            }
            currentnode.addFruit(fruit_);
        }

        addCore(fruit_);
    }

    private void delBranch(Info fruit_, Deque queue) {
        if (queue.size() > 1) {
            ACNode endnode = (ACNode) queue.pollLast();
            if (endnode.delFruit(fruit_) && endnode.countFruit() == 0) {
                ACNode currentnode = endnode;
                String leaf = currentnode.getLeaf();
                while (currentnode.countBranch() == 0 && currentnode.countFruit() == 0
                        && (currentnode = (ACNode) queue.pollLast()) != null) {
                    currentnode.delBranch(leaf);
                    leaf = currentnode.getLeaf();
                }
            }
        }
    }

    public void delBranch(Info fruit_) {
        Deque queue = new ArrayDeque<ACNode>();
        String[] strarray = fruit_.getString();
        for (String str : strarray) {
            String[] strs = str.split(" ");
            ACNode currentnode = this.root;
            queue.add(currentnode);
            for (int i = 0; i < strs.length; ++i) {
                if (currentnode.checkBranch(strs[i]) != null) {
                    currentnode = currentnode.checkBranch(strs[i]);
                    queue.add(currentnode);
                } else {
                    break;
                }
            }
            delBranch(fruit_, queue);
            queue.clear();
        }

        strarray = fruit_.getPinYin();
        for (String str : strarray) {
            str = str.replaceAll(" ", "");
            str = StringMachine.insertBlank(str);
            String[] strs = str.split(" ");
            ACNode currentnode = this.root;
            queue.add(currentnode);
            for (int i = 0; i < strs.length; ++i) {
                if (currentnode.checkBranch(strs[i]) != null) {
                    currentnode = currentnode.checkBranch(strs[i]);
                    queue.add(currentnode);
                } else {
                    break;
                }
            }
            delBranch(fruit_, queue);
            queue.clear();
        }

        delCore(fruit_);
    }

    public void addBud() {
        ACNode currentnode = this.root;
        currentnode.addBud(null);

//        先获取根节点的孩子节点
        Queue queue = new ArrayDeque<ACNode>();
        Iterator<String> iter = currentnode.branches.keySet().iterator();
        while (iter.hasNext()) {
            String key = iter.next();
            ACNode tmpnode = (ACNode) (currentnode.branches.get(key));
            tmpnode.addBud(currentnode);
            queue.add(tmpnode);
        }
//     在遍历队列里的节点建立fail链
        while ((currentnode = (ACNode) queue.poll()) != null) {
            // System.out.println(currentnode.leaf);
            iter = currentnode.branches.keySet().iterator();
            while (iter.hasNext()) {
                String key = iter.next();
                ACNode tmpnode = (ACNode) (currentnode.branches.get(key));
                addBud(tmpnode, currentnode);
                queue.add(tmpnode);
            }
        }
    }

    /**
     * 建立子节点与父节点之间的联系
     * @param currentnode
     * @param parentnode
     */
    public void addBud(ACNode currentnode, ACNode parentnode) {
//        先找到对应的父节点的fail指针指向的节点
        ACNode budnode = parentnode.bud;
        String key = currentnode.getLeaf();
//        判断fail链是否存在与currentnode节点一样的key值
        while (budnode != null && budnode.checkBranch(key) == null) {
            budnode = budnode.bud;
        }
//        找到fail指向的节点
        if (budnode != null) {
            currentnode.addBud((ACNode) (budnode.branches.get(key)));
        } else {
//            没找到指向根节点
            currentnode.addBud(this.root);
        }
        currentnode.addLink(currentnode.bud);
    }

    public List findBranch(String text) {
        String[] strs = text.split(" ");
        ACNode currentnode = this.root;
        List ret = new ArrayList<Match>();
        for (int i = 0; i < strs.length; ++i) {
            if (currentnode.checkBranch(strs[i]) != null) {
                currentnode = currentnode.checkBranch(strs[i]);
                for (Iterator<Info> iter = currentnode.fruits.iterator(); iter.hasNext();) {
                    Match match = new Match(i, currentnode.getLevel(), (Info) iter.next());
                    ret.add(match);
                }
                for (Iterator<ACNode> iter = currentnode.link.iterator(); iter.hasNext();) {
                    ACNode tmpnode = iter.next();
                    for (Iterator<Info> iter_ = tmpnode.fruits.iterator(); iter_.hasNext();) {
                        Match match = new Match(i, tmpnode.getLevel(), (Info) iter_.next());
                        ret.add(match);
                    }
                }
            } else if (currentnode.bud != null) {
                currentnode = currentnode.bud;
                i--;
            }
        }

        return ret;
    }

    /**
     * 将敏感词替换为*
     * @param text
     * @return
     */
//    public static String replaceSensitive(String text) {
//        String[] strs = text.split(" ");
//        AcNode currentNode = root;
//        StringBuilder result = new StringBuilder();
//        for(int i = 0; i < strs.length; i ++) {
//            if(currentNode.checkBranch(strs[i]) != null) {
//                AcNode originalNode = currentNode;
//                AcNode longestMatchedNode = null;
//                String longestSensitiveWord = "";
//
//                while (originalNode.checkBranch(strs[i]) != null) {
//                    originalNode = originalNode.checkBranch(strs[i]);
//                    if (!originalNode.getFruits().isEmpty()) {
//                        // 当找到敏感词时，记录最长的匹配
//                        if (strs[i].length() > longestSensitiveWord.length()) {
//                            longestMatchedNode = originalNode;
//                            longestSensitiveWord = strs[i];
//                        }
//                    }
//                }
//
//                if (longestMatchedNode != null) {
//                    // 将最长敏感词替换为*
//                    String replacement = StringMachine.replaceWithCharacter(longestSensitiveWord,"*");
//                    result.append(replacement);
//                    // 移动当前节点到最长匹配的节点
//                    currentNode = longestMatchedNode;
//                    // 跳过中间的词
//                    while (i + 1 < strs.length && strs[i + 1].equals(strs[i])) {
//                        i++;
//                    }
//                } else {
//                    result.append(strs[i]);
//                }
//
//                result.append(" ");
//            } else if(currentNode.getBud() != null) {
//                currentNode = currentNode.getBud();
//                i --;
//            }
//            else {
//                result.append(strs[i]);
//                result.append(" ");
//            }
//        }
//        return result.toString().trim();
//    }
}
