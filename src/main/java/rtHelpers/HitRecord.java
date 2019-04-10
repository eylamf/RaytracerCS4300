package rtHelpers;

import org.joml.Matrix4f;
import org.joml.Vector4f;
import util.Material;

public class HitRecord implements Comparable<HitRecord> {
    public float tEnt, tEx;
    public Vector4f intersectionIn, intersectionOut, normalIn, normalOut;
    public Material material;
    public Matrix4f transform;

    public HitRecord(float tEnt, float tEx, Vector4f intersectionIn, Vector4f intersectionOut, Vector4f normalIn, Vector4f normalOut, Material mat, Matrix4f trans) {
        this.tEnt = tEnt;
        this.tEx = tEx;
        this.intersectionIn = intersectionIn;
        this.intersectionOut = intersectionOut;
        this.normalIn = normalIn;
        this.normalOut = normalOut;
        this.material = mat;
        this.transform = trans;
    }


    @Override
    public int compareTo(HitRecord o) {
        return (int) (this.tEnt - o.tEnt);
    }

    public Vector4f get(Vector4f vector4f) {
        return new Vector4f(vector4f);
    }
}
