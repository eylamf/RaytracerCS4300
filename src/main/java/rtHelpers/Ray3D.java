package rtHelpers;

import org.joml.Matrix4f;
import org.joml.Vector4f;

public class Ray3D {
    public Vector4f startPoint;
    public Vector4f direction;

    public Ray3D(Vector4f sp, int i, int j, int w, int h, float theta) {
        this.startPoint = new Vector4f(sp.x, sp.y, sp.z, 1);
        this.direction = new Vector4f((i - (w / 2)) - sp.x, (j - (h / 2)) - sp.y, (float) ((-0.5f * h) / Math.tan(theta / 2f)) - sp.z, 0);
        this.direction.normalize();
    }

    public Ray3D(Vector4f sp, Vector4f dir) {
        this.startPoint = new Vector4f(sp);
        this.direction = new Vector4f(dir);
        this.direction.normalize();
    }

    public Ray3D(Ray3D ray3D) {
        this.startPoint = new Vector4f(ray3D.startPoint);
        this.direction = new Vector4f(ray3D.direction);
        this.direction.normalize();
    }

    public void transform(Matrix4f trans) {
        Matrix4f t = new Matrix4f();
        trans.invert(t);

        this.startPoint = t.transform(this.startPoint);
        this.direction = t.transform(this.direction);
        this.direction.normalize();
    }

    public Vector4f getStartPoint() {
        return new Vector4f(this.startPoint);
    }

    public Vector4f getDirection() {
        return new Vector4f(this.direction);
    }
}
