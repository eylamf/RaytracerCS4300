package rtHelpers;

import org.joml.Vector4f;
import util.Material;

public class HitRecord {
    private float time;
    private Vector4f startPoint, normal, texture;
    private Material material;

    public HitRecord() {
        time = Float.MAX_VALUE;
        startPoint = new Vector4f(0, 0, 0, 1);
        normal = new Vector4f(0, 0, 1, 0);
        texture = new Vector4f(1, 1, 1, 1);
        material = new Material();
    }

    public float getT() {
        return this.time;
    }

    public void setT(float t) {
        this.time = t;
    }

    public Vector4f getStartPoint() {
        return new Vector4f(this.startPoint);
    }

    public void setStartPoint(Vector4f sp) {
        this.startPoint = new Vector4f(sp.x, sp.y, sp.z, sp.w);
    }

    public Vector4f getNormal() {
        return new Vector4f(this.normal);
    }

    public void setNormal(Vector4f n) {
        this.normal = new Vector4f(n.x, n.y, n.z, n.w);
    }

    public Vector4f getTexture() {
        return new Vector4f(this.texture);
    }

    public void setTexture(Vector4f text) {
        this.texture = new Vector4f(text);
    }

    public Material getMaterial() {
        return new Material(material);
    }

    public void setMaterial(Material mat) {
        this.material = new Material(mat);
    }

    public boolean isHit() {
        return this.time < Float.MAX_VALUE;
    }
}
