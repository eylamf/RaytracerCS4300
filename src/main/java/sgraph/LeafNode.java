package sgraph;

import com.jogamp.opengl.GL3;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import rtHelpers.HitRecord;
import rtHelpers.Ray3D;
import util.Material;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Stack;

/**
 * This node represents the leaf of a scene graph. It is the only type of node that has
 * actual geometry to render.
 * @author Amit Shesh
 */
public class LeafNode extends AbstractNode
{
    /**
     * The name of the object instance that this leaf contains. All object instances are stored
     * in the scene graph itself, so that an instance can be reused in several leaves
     */
    protected String objInstanceName;
    /**
     * The material associated with the object instance at this leaf
     */
    protected util.Material material;

    protected String textureName;

    public LeafNode(String instanceOf,IScenegraph graph,String name)
    {
        super(graph,name);
        this.objInstanceName = instanceOf;
    }



    /*
	 *Set the material of each vertex in this object
	 */
    @Override
    public void setMaterial(util.Material mat)
    {
        material = new util.Material(mat);
    }

    /**
     * Set texture ID of the texture to be used for this leaf
     * @param name
     */
    @Override
    public void setTextureName(String name)
    {
        textureName = name;
    }

    /*
     * gets the material
     */
    public util.Material getMaterial()
    {
        return material;
    }

    @Override
    public INode clone()
    {
        LeafNode newclone = new LeafNode(this.objInstanceName,scenegraph,name);
        newclone.setMaterial(this.getMaterial());
        return newclone;
    }


    /**
     * Delegates to the scene graph for rendering. This has two advantages:
     * <ul>
     *     <li>It keeps the leaf light.</li>
     *     <li>It abstracts the actual drawing to the specific implementation of the scene graph renderer</li>
     * </ul>
     * @param context the generic renderer context {@link sgraph.IScenegraphRenderer}
     * @param modelView the stack of modelview matrices
     * @throws IllegalArgumentException
     */
    @Override
    public void draw(IScenegraphRenderer context,Stack<Matrix4f> modelView) throws IllegalArgumentException
    {
        if (objInstanceName.length()>0)
        {
            context.drawMesh(objInstanceName,material,textureName,modelView.peek());
        }
    }

    @Override
    public List<HitRecord> raycast(Ray3D ray, Stack<Matrix4f> transformations) throws IllegalArgumentException {
        List<HitRecord> hitRecords = new ArrayList();
        List<HitRecord> objectHits;

        switch (objInstanceName) {
            case "box":
                objectHits = intersectSquare(ray, transformations);

                if (objectHits.size() > 0) {
                    hitRecords.addAll(objectHits);
                }

                break;
            case "box-outside":
                objectHits = intersectSquare(ray, transformations);

                if (objectHits.size() > 0) {
                    hitRecords.addAll(objectHits);
                }

                break;
            case "sphere":
                objectHits = intersectSphere(ray, transformations);

                if (objectHits.size() > 0) {
                    hitRecords.addAll(objectHits);
                }

                break;
        }

        return hitRecords;
    }

    private List<HitRecord> intersectSquare(Ray3D ray, Stack<Matrix4f> mv) {
        List<HitRecord> hitRecords = new ArrayList();
        Ray3D newRay = new Ray3D(ray);
        Matrix4f transformation = new Matrix4f(mv.peek());

        newRay.transform(transformation);

        HitRecord hit;
        float tMax = Float.MAX_VALUE;
        float tMin = -Float.MAX_VALUE;

        // tx1 = (-0.5 - sx) / vx
        float tx1 = (-0.5f - newRay.getStartPoint().x) / newRay.getDirection().x;
        // tx2 = (0.5 - sx) / vx
        float tx2 = (0.5f - newRay.getStartPoint().x) / newRay.getDirection().x;
        // max and min t
        float tMaxX = Math.max(tx1, tx2);
        float tMinX = Math.min(tx1, tx2);

        // Repeat for y and z
        float ty1 = (-0.5f - newRay.getStartPoint().y) / newRay.getDirection().y;
        float ty2 = (0.5f - newRay.getStartPoint().y) / newRay.getDirection().y;
        float tMaxY = Math.max(ty1, ty2);
        float tMinY = Math.min(ty1, ty2);

        float tz1 = (-0.5f - newRay.getStartPoint().z) / newRay.getDirection().z;
        float tz2 = (0.5f - newRay.getStartPoint().z) / newRay.getDirection().z;
        float tMaxZ = Math.max(tz1, tz2);
        float tMinZ = Math.min(tz1, tz2);

        float minT = Math.max(tMinX, Math.max(tMinY, Math.max(tMinZ, tMin)));
        float maxT = Math.min(tMaxX, Math.min(tMaxY, Math.min(tMaxZ, tMax)));

        float tEnt, tEx;

        if (minT != maxT && minT > 0 && minT <= maxT) {
            tEnt = minT;

            if (maxT != tMax) {
                tEx = maxT;

                Vector4f intersectionIn = newRay.getStartPoint().add(newRay.getDirection()).mul(minT);
                Vector4f intersectionOut = newRay.getStartPoint().add(newRay.getDirection().mul(maxT));
                Vector4f normalIn = getNormalForBox(intersectionIn);
                Vector4f normalOut = getNormalForBox(intersectionOut);
                Material material = this.getMaterial();
                hit = new HitRecord(tEnt, tEx, intersectionIn, intersectionOut, normalIn, normalOut, material, new Matrix4f(mv.peek()));
                hitRecords.add(hit);
            }
        } else {
            tEnt = maxT;
        }

        return hitRecords;
    }

    public Vector4f getNormalForBox(Vector4f intersection) {
        float nx = 0;
        float ny = 0;
        float nz = 0;

        if (intersection.x == -0.5f || intersection.x == 0.5f) {
            nx = intersection.x;
        }

        if (intersection.y == -0.5f || intersection.y == 0.5f) {
            ny = intersection.y;
        }

        if (intersection.z == -0.5f || intersection.z == 0.5f) {
            nz = intersection.z;
        }

        return new Vector4f(nx, ny, nz, 0).normalize();
    }

    private List<HitRecord> intersectSphere(Ray3D ray, Stack<Matrix4f> mv) {
        List<HitRecord> hitRecords = new ArrayList();
        Ray3D newRay = new Ray3D(ray);
        Matrix4f transformation = new Matrix4f(mv.peek());

        newRay.transform(transformation);

        Vector4f startPoint = newRay.getStartPoint();
        Vector4f direction = newRay.getDirection();

        HitRecord hit;

        float tMax = Float.MAX_VALUE;
        float tMin = -Float.MAX_VALUE;

        //
        float a = 1;
        // 2(vxsx, vysy, vzsz)
        float b = 2 * (startPoint.dot(direction));
        // = sx^2 + sy^2 + sz^2 - 1
        float c = (float) (Math.pow(startPoint.x, 2) + Math.pow(startPoint.y, 2) + Math.pow(startPoint.z, 2) - 1);

        List<Float> t = quadratic(a, b, c);

        if (t.size() == 2) {
            if (t.get(0) != Float.MAX_VALUE) {
                float tEnt = t.get(0);
                float tEx = t.get(1);

                if (tEnt > 0) {
                    Vector4f intersectionIn = new Vector4f(startPoint.add(direction.mul(tEnt)));
                    Vector4f intersectionOut = new Vector4f(startPoint.add(direction.mul(tEx)));
                    hit = new HitRecord(tEnt, tEx, intersectionIn, intersectionOut, intersectionIn, intersectionOut, this.getMaterial(), new Matrix4f(mv.peek()));
                    hitRecords.add(hit);
                }
            }
        } else if (t.size() == 1) {
            float tEnt = t.get(0);

            if (tEnt > 0) {
                Vector4f intersectionIn = new Vector4f(newRay.getStartPoint().add(newRay.getDirection().mul(tEnt)));
                hit = new HitRecord(tEnt, tEnt, intersectionIn, intersectionIn, intersectionIn, intersectionIn, this.getMaterial(), new Matrix4f(mv.peek()));
                hitRecords.add(hit);
            }
        }

        return hitRecords;
    }

    public List<Float> quadratic(float a, float b, float c) {
        List<Float> t = new ArrayList();
        float t1, t2;
        float numerator = (float) (Math.pow(b, 2) - (4 * a * c));

        if (numerator < 0) {
            t1 = Float.MAX_VALUE;
            t2 = Float.MAX_VALUE;
            t.add(t1);
            t.add(t2);
        } else {
            t1 = (float)(-b + Math.sqrt(numerator)) / (2 * a);
            t2 = (float)(-b - Math.sqrt(numerator)) / (2 * a);
            if (t1 > t2) {
                t.add(t2);
                t.add(t1);
            }
            else if (t1 < t2) {
                t.add(t1);
                t.add(t2);
            }
            else {
                t.add(t1);
            }
        }

        return t;
    }

}
