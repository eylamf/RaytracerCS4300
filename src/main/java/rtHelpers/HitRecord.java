package rtHelpers;

import org.joml.Vector4f;
import util.Material;

public class HitRecord {
    public float time;
    public Vector4f startPoint, normal, texture;
    public Material material;

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
        return this.startPoint;
    }

    public void setStartPoint(Vector4f sp) {
        this.startPoint = sp;
    }

    public Vector4f getNormal() {
        return this.normal;
    }

    public void setNormal(Vector4f n) {
        this.normal = n;
    }

    public Vector4f getTexture() {
        return texture;
    }

    public void setTexture(Vector4f text) {
        this.texture = text;
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
