/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package merkletree;

import java.io.Serializable;

import java.nio.charset.StandardCharsets;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang.ArrayUtils;

import org.json.JSONArray;

/**
 *
 * @author snake
 */
public class TreeCertificate implements Serializable {
    
    private final int id;
    private final long timestamp;
    private final byte[] signature;
    
    private static final String hashAlgorithm = "SHA-1";
    
    public TreeCertificate(int id, long timestamp, byte[] signature) {
        this.id = id;
        this.timestamp = timestamp;
        this.signature = signature;
    }

    public int getId() {
        return id;
    }
    
    public long getTimestamp() {
        return timestamp;
    }

    public byte[] getSignature() {
        return signature;
    }
 
    @Override
    public boolean equals(Object e) {
        if (e instanceof TreeCertificate) {
            
            TreeCertificate other = (TreeCertificate) e;
            
            return (timestamp == other.timestamp);
        }
        return false;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 17 * hash + this.id;
        hash = 17 * hash + (int) (this.timestamp ^ (this.timestamp >>> 32));
        hash = 17 * hash + Arrays.hashCode(this.signature);
        return hash;
    }
    
    @Override
    public String toString() {        
        
        return "[" + id + ", " + timestamp + ", " + Arrays.toString(signature) + "]";
    }        
                
    public static byte[] concatenate(int id, long timestamp, List roots) {
        byte[] buffer = new byte[] {
                    (byte)(id >>> 24),
                    (byte)(id >>> 16),
                    (byte)(id >>> 8),
                    (byte)id,
                    (byte)(timestamp >>> 56),
                    (byte)(timestamp >>> 48),
                    (byte)(timestamp >>> 40),
                    (byte)(timestamp >>> 32),
                    (byte)(timestamp >>> 24),
                    (byte)(timestamp >>> 16),
                    (byte)(timestamp >>> 8),
                    (byte)timestamp};

        byte[][] array = new byte[roots.size()][];
        roots.toArray(array);

        for (byte[] a : array) 
            buffer = ArrayUtils.addAll(buffer, a);

        return buffer;
    }

    public static JSONArray getJSON(LinkedList<LinkedList<Object>> rows) {
        JSONArray result = new JSONArray();

        for (int i = 0; i < rows.size(); i++) {


            JSONArray row = new JSONArray();

            for (int j = 0; j < rows.get(i).size(); j++) {

                row.put(j, rows.get(i).get(j));
            }

            result.put(i, row);

        }

        return result;
    }

    public static Leaf[] jsonToLeafs(JSONArray rs) {

        if (rs == null) return null;

        int nRows = 2;

        if (rs.length() > 1) {
            nRows = nextPowerOf2(rs.length()); // this ensures that the number of data blocks is a power of 2
        }

        // Create data blocks to be assigned to leaf nodes
        Leaf[] leafs = new Leaf[nRows];

        for (int i = 0; i < nRows; i++) {

            List<byte[]> l = new LinkedList<>();

            if (i < rs.length()) {

                JSONArray col = (JSONArray) rs.get(i);

                for (int j = 0; j < col.length(); j++) {
                    l.add(col.get(j).toString().getBytes(StandardCharsets.UTF_8));
                }

            } else {
                l.add(new byte[0]);
            }

            leafs[i] = new Leaf(l);
        }

        return leafs;
    }

    public static MerkleTree[] getFirstLevel(Leaf[] leafs) {

        if (leafs == null || leafs.length < 2) return null;

        MerkleTree[] m = new MerkleTree[leafs.length/2];

        // Define the message digest algorithm to use
        MessageDigest md = null;
        try 
        {
                md = MessageDigest.getInstance(hashAlgorithm);
        } 
        catch (NoSuchAlgorithmException e) 
        {
                // Should never happen, we specified SHA, a valid algorithm
                assert false;
        } 

        for (int i = 0, j = 0; i < leafs.length; i += 2, j++) {                        

                    m[j] = new MerkleTree(md);
                    m[j].add(leafs[i], leafs[i+1]);
        }

        return m;
    }

    public static MerkleTree[] getNextLevel(MerkleTree[] branches) {

        if (branches == null || branches.length < 2) return null;

        MerkleTree[] m = new MerkleTree[branches.length/2];

        // Define the message digest algorithm to use
        MessageDigest md = null;
        try 
        {
                md = MessageDigest.getInstance(hashAlgorithm);
        } 
        catch (NoSuchAlgorithmException e) 
        {
                // Should never happen, we specified SHA, a valid algorithm
                assert false;
        } 

        for (int i = 0, j = 0; i < branches.length; i += 2, j++) {
                    m[j] = new MerkleTree(md);
                    m[j].add(branches[i], branches[i+1]);
        }

        return m;
    }

    public static MerkleTree makeMerkleTree(Leaf[] leafs) {

        MerkleTree[] branches = null; //new MerkleTree[leafs.length];           

        do {                                

            if (branches == null) {

                branches = getFirstLevel(leafs);

            } else {
                branches = getNextLevel(branches);
            }


        } while(branches.length > 1);


        return branches[0];

    }

    public static MerkleTree getMerkleTree(LinkedList<LinkedList<Object>> rows) {

        return makeMerkleTree(jsonToLeafs(getJSON(rows)));

    }        


    public static int nextPowerOf2(int n) {
        int p = (n == 0 ? 0 : 32 - Integer.numberOfLeadingZeros(n - 1));
        return (int) Math.pow(2, p);
    }        

}
