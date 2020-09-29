/*
 * Copyright 2017 SNOMED International, http://snomed.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.snomed.otf.owltoolkit.normalform.transitive;

import com.google.common.graph.GraphBuilder;
import com.google.common.graph.MutableGraph;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;

import java.util.Collections;
import java.util.Set;

public class NodeGraph {

	private final Set<Long> nodes = new LongOpenHashSet();
	private final MutableGraph<Long> graph = GraphBuilder.directed().build();

	public void addParent(Long conceptId, Long parentId) {
//		System.out.println("Add parent " + conceptId + " -> " + parentId);
		nodes.add(conceptId);
		nodes.add(parentId);
		graph.putEdge(parentId, conceptId);
	}

	public Set<Long> getAncestors(long conceptId) {
		if (!nodes.contains(conceptId)) {
			return Collections.emptySet();
		}
		return graph.predecessors(conceptId);
	}

	public void dump() {
//		for (Long aLong : nodeMap.keySet()) {
//			Node node = nodeMap.get(aLong);
//			printAll(node, "");
//			System.out.println();
//		}
	}

//	private void printAll(Node node, String indent) {
//		System.out.println(indent + " " + node);
//		for (Node parent : node.getParents()) {
//			printAll(parent, indent + "-");
//		}
//	}
}
