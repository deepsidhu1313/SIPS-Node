/* 
 * Copyright (C) 2017 Navdeep Singh Sidhu
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package in.co.s13.SIPS.datastructure.graph;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.PriorityQueue;

/**
 *
 * @author nika
 */
public class Dijkstra {

    public static void computePaths(Vertex source) {
        source.minDistance = 0.;
        PriorityQueue<Vertex> vertexQueue = new PriorityQueue<Vertex>();
        vertexQueue.add(source);

        while (!vertexQueue.isEmpty()) {
            Vertex u = vertexQueue.poll();

            // Visit each edge exiting u
            for (Edge e : u.adjacencies) {
                Vertex v = e.target;
                double weight = e.weight;
                double distanceThroughU = u.minDistance + weight;
                if (distanceThroughU < v.minDistance) {
                    vertexQueue.remove(v);

                    v.minDistance = distanceThroughU;
                    v.previous = u;
                    vertexQueue.add(v);
                }
            }
        }
    }

    public static List<Vertex> getShortestPathTo(Vertex target) {
        List<Vertex> path = new ArrayList<Vertex>();
        for (Vertex vertex = target; vertex != null; vertex = vertex.previous) {
            path.add(vertex);
        }

        Collections.reverse(path);
        return path;
    }

    public static void main(String[] args) {
        // mark all the vertices 
        Vertex A = new Vertex("A");
        Vertex B = new Vertex("B");
        Vertex D = new Vertex("D");
        Vertex F = new Vertex("F");
        Vertex K = new Vertex("K");
        Vertex J = new Vertex("J");
        Vertex M = new Vertex("M");
        Vertex O = new Vertex("O");
        Vertex P = new Vertex("P");
        Vertex R = new Vertex("R");
        Vertex Z = new Vertex("Z");

        // set the edges and weight
        A.adjacencies = new Edge[]{new Edge(M, 8)};
        B.adjacencies = new Edge[]{new Edge(D, 11)};
        D.adjacencies = new Edge[]{new Edge(B, 11)};
        F.adjacencies = new Edge[]{new Edge(K, 23)};
        K.adjacencies = new Edge[]{new Edge(O, 40)};
        J.adjacencies = new Edge[]{new Edge(K, 25)};
        M.adjacencies = new Edge[]{new Edge(R, 8)};
        O.adjacencies = new Edge[]{new Edge(K, 40)};
        P.adjacencies = new Edge[]{new Edge(Z, 18)};
        R.adjacencies = new Edge[]{new Edge(P, 15)};
        Z.adjacencies = new Edge[]{new Edge(P, 18)};

        computePaths(A); // run Dijkstra
        System.out.println("Distance to " + Z + ": " + Z.minDistance);
        List<Vertex> path = getShortestPathTo(Z);
        System.out.println("Path: " + path);
    }

}
