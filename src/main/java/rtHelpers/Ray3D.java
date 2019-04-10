package rtHelpers;

import org.joml.Vector4f;

public class Ray3D {
    private Vector4f startPoint, direction;

    public Ray3D() {
        this.startPoint = new Vector4f(0, 0, 0, 1);
        this.direction = new Vector4f(0, 0, 1, 0);
    }

    public Ray3D(Vector4f sp, Vector4f dir) {
        this.startPoint = new Vector4f(sp);
        this.direction = new Vector4f(dir);
    }

    public Ray3D(Ray3D ray) {
        this.startPoint = ray.getStartPoint();
        this.direction = ray.getDirection();
    }

    public Vector4f getStartPoint() {
        return new Vector4f(this.startPoint);
    }

    public Vector4f getDirection() {
        return new Vector4f(this.direction);
    }

    public void setStartPoint(Vector4f sp) {
        this.startPoint = new Vector4f(sp.x, sp.y, sp.z, sp.w);
    }

    public void setDirection(Vector4f dir) {
        this.direction = new Vector4f(dir.x, dir.y, dir.z, dir.w);
    }
}
