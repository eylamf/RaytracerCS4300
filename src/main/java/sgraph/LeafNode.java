package sgraph;

import com.jogamp.opengl.GL3;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import rtHelpers.HitRecord;
import rtHelpers.Ray3D;
import util.Material;
import util.TextureImage;

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

    @Override
    public void intersect(Ray3D ray, Stack<Matrix4f> mv, HitRecord hitRecord, Map<String, TextureImage> textureImageMap) {
        Ray3D newRay = new Ray3D();

        Matrix4f nodeToWorld = new Matrix4f(mv.peek());

        Matrix4f worldToNode = new Matrix4f(nodeToWorld).invert();

        newRay.startPoint = new Vector4f(ray.startPoint);
        newRay.direction = new Vector4f(ray.direction);

        newRay.startPoint = worldToNode.transform(newRay.startPoint);
        newRay.direction = worldToNode.transform(newRay.direction);


        if (objInstanceName.equals("sphere")) {
            intersectSphere(ray, newRay, hitRecord, textureImageMap, worldToNode);
        } else if (objInstanceName.equals("box") || objInstanceName.equals("box-outside")) {
            intersectRect(ray, newRay, hitRecord, textureImageMap, worldToNode);
        }

    }

    /*
    If sphere centered at origi with r =1
    a = vx^2 + vy^2 + vz^2
    b = (vx*sx + vy*sy + vz * sz) * 2
    c = sx^2 + sy^2 + sz^ 2 -1

    If its not...
    b = 2vx(sx - cx) + 2vy(sy - cy) + 2vz(sz - cz)
    c = (sx - cx)^2 + (sy - cy)^2 + (sz -cz)^2 - r^2
     */
    private void intersectSphere(Ray3D ray, Ray3D newRay, HitRecord hitRecord, Map<String, TextureImage> textureImageMap, Matrix4f worldToNode) {
        float a = newRay.direction.lengthSquared();
        float b = newRay.startPoint.dot(newRay.direction) * 2;
        float c = newRay.startPoint.lengthSquared()-1 - 1;

        // Quadratic formula
        if ((Math.pow(b, 2) - 4 * a * c) >= 0) {
            float t1 = (-b + (float)Math.sqrt(Math.pow(b, 2) - 4 * a * c)) / (2 * a);
            float t2 = (-b - (float)Math.sqrt(Math.pow(b, 2) - 4 * a * c))/ (2 * a);

            float t;

            if (t1 >= 0) {
                if (t2>=0) {
                    t = Math.min(t1,t2);
                } else {
                    t = t1;
                }
            } else {
                if (t2 >= 0) {
                    t = t2;
                } else {
                    return;
                }
            }

            if (t < hitRecord.time) {
                hitRecord.time = t;
                hitRecord.startPoint = new Vector4f(ray.startPoint.x + t * ray.direction.x,
                        ray.startPoint.y + t * ray.direction.y,ray.startPoint.z + t * ray.direction.z,1);

                getNormalForSphere(newRay, hitRecord, t, worldToNode);

                hitRecord.setMaterial(material);

                // Texture color
                Vector4f textureColor = new Vector4f(1,1,1,1);

                TextureImage textureImage = textureImageMap.get(textureName);
                Vector4f start = newRay.startPoint;
                Vector4f dir = newRay.direction;

                Vector4f pointInNode = new Vector4f(start.x + dir.x * t, start.y + dir.y * t, start.z + dir.z * t, 1);

                if (textureImage != null) {
                    wrapSphere(pointInNode, textureColor, textureImage, hitRecord);
                }
            }

        }
    }

    // Calculate normal vector for sphere
    private void getNormalForSphere(Ray3D newRay, HitRecord hitRecord, float t, Matrix4f worldToNode) {
        hitRecord.normal = new Vector4f(newRay.startPoint.x + t * newRay.direction.x,newRay.startPoint.y + t * newRay.direction.y,newRay.startPoint.z + t * newRay.direction.z,0);

        hitRecord.normal = new Matrix4f(worldToNode).transpose().transform(hitRecord.normal);
        hitRecord.normal = new Vector4f(new Vector3f(hitRecord.normal.x, hitRecord.normal.y, hitRecord.normal.z).normalize(),0);
    }

    // Wrap texture onto sphere coords
    /*
    0 <= theta <= 2PI
    theta = tan^-1(z/x)
     */
    private void wrapSphere(Vector4f pointInNode, Vector4f textureColor, TextureImage textureImage, HitRecord hitRecord) {
        // Sphere coords
        float sx = pointInNode.x;
        float sy = pointInNode.y;
        float sz = pointInNode.z;

        float phi = (float) Math.asin(sy);
        phi = phi - (float) Math.PI / 2;
        float theta = (float) Math.atan2((double) sz, (double) sx);
        theta  = theta +(float) Math.PI;
        float x = (float)(0.5f - (theta / (2 * Math.PI))) ;
        float y = (1 - (phi / ((float) Math.PI)));

        textureColor = textureImage.getColor(x, y);

        hitRecord.setTexture(textureColor);
    }

    /*
    From notes:
    Need to find t such that s + tv
    t = -(asx + bsy + csz + d) / (avx + bvy + cvz)

    tx1 = (-0.5 - sx) / vx
    tx2 = (-0.5 - sx) / vx
    txMin = min(tx1, tx2)
    txMax = mac(tx1, tx2)
    .
    .
    .
    same for y and z with corresponding "s" and "v"
     */
    private void intersectRect(Ray3D ray, Ray3D newRay, HitRecord hitRecord, Map<String, TextureImage> textureImageMap, Matrix4f worldToNode) {
        float txMax;
        float tyMax;
        float tzMax;
        float txMin;
        float tyMin;
        float tzMin;

        // For each one, checkout again 0.0001f to avoid black specs on the object
        if (Math.abs(newRay.direction.x) < 0.0001f) {
            if ((newRay.startPoint.x <= 0.5f) || (newRay.startPoint.x >= -0.5f)) {
                txMin = -Float.MAX_VALUE;
                txMax = Float.MAX_VALUE;
            } else {
                return;
            }

        } else {
            float t1 = (-0.5f - newRay.startPoint.x) / newRay.direction.x;
            float t2 = (0.5f - newRay.startPoint.x) / newRay.direction.x;

            txMin = Math.min(t1,t2);
            txMax = Math.max(t1,t2);
        }

        if (Math.abs(newRay.direction.y) < 0.0001f) {
            if ((newRay.startPoint.y <= 0.5f) || (newRay.startPoint.y >= -0.5f)) {
                tyMin = -Float.MAX_VALUE;
                tyMax = Float.MAX_VALUE;
            } else {
                return;
            }
        } else {
            float t1 = (-0.5f - newRay.startPoint.y) / newRay.direction.y;
            float t2 = (0.5f - newRay.startPoint.y) / newRay.direction.y;
            tyMin = Math.min(t1,t2);
            tyMax = Math.max(t1,t2);
        }

        if (Math.abs(newRay.direction.z) < 0.0001f) {
            if ((newRay.startPoint.z <= 0.5f) || (newRay.startPoint.z >= -0.5f)) {
                tzMin = Float.NEGATIVE_INFINITY;
                tzMax = Float.POSITIVE_INFINITY;
            } else {
                return;
            }
        } else {
            float t1 = (-0.5f - newRay.startPoint.z) / newRay.direction.z;
            float t2 = (0.5f - newRay.startPoint.z) / newRay.direction.z;
            tzMin = Math.min(t1,t2);
            tzMax = Math.max(t1,t2);
        }

        float minT, maxT;

        minT = Math.max(txMin, Math.max(tyMin, tzMin));
        maxT = Math.min(txMax, Math.min(tyMax, tzMax));

        if ((minT < maxT) && (maxT > 0)) {
            float t;
            if (minT > 0) {
                t = minT;
            } else {
                t = maxT;
            }

            if (t < hitRecord.time) {
                hitRecord.time = t;

                hitRecord.startPoint = new Vector4f(ray.startPoint.x + t * ray.direction.x,
                        ray.startPoint.y + t * ray.direction.y,
                        ray.startPoint.z + t * ray.direction.z,
                        1);

                Vector4f pointInNode = new Vector4f(newRay.startPoint.x + t * newRay.direction.x,
                        newRay.startPoint.y + t * newRay.direction.y,
                        newRay.startPoint.z + t * newRay.direction.z,
                        1);

                // Set the normal vector here
                getNormalForBox(pointInNode, hitRecord, worldToNode);

                hitRecord.setMaterial(material);

                // Texture color
                Vector4f textureColor = new Vector4f(1,1,1,1);

                TextureImage textureImage = textureImageMap.get(textureName);

                if (textureImage != null) {
                    // Go through each face of the cube and wrap the texture coords
                    wrapFaces(pointInNode, textureColor, textureImage, hitRecord);
                }
            }
        }
    }

    private void getNormalForBox(Vector4f pointInNode, HitRecord hitRecord, Matrix4f worldToNode) {
        if (Math.abs(pointInNode.x - 0.5f) < 0.001) {
            hitRecord.normal.x = 1;

        } else if (Math.abs(pointInNode.x + 0.5f) < 0.001) {
            hitRecord.normal.x = -1;
        } else {
            hitRecord.normal.x = 0;
        }

        if (Math.abs(pointInNode.y - 0.5f) < 0.001) {
            hitRecord.normal.y = 1;
        } else if (Math.abs(pointInNode.y + 0.5f) < 0.001) {
            hitRecord.normal.y = -1;
        } else {
            hitRecord.normal.y = 0;
        }

        if (Math.abs(pointInNode.z - 0.5f) < 0.001) {
            hitRecord.normal.z = 1;
        } else if (Math.abs(pointInNode.z + 0.5f) < 0.001) {
            hitRecord.normal.z = -1;
        } else {
            hitRecord.normal.z = 0;
        }

        hitRecord.normal.w = 0;
        hitRecord.normal.normalize();


        hitRecord.normal = new Matrix4f(worldToNode).transpose().transform(hitRecord.normal);
        hitRecord.normal = new Vector4f(new Vector3f(hitRecord.normal.x,hitRecord.normal.y,hitRecord.normal.z).normalize(),0);
    }

    private void wrapFaces(Vector4f pointInNode, Vector4f textureColor, TextureImage textureImage, HitRecord hitRecord) {
        float bx = pointInNode.x;
        float by = pointInNode.y;
        float bz = pointInNode.z;

        if (bx <= 0.51f && bx >= -0.51f && by <= 0.51f && by >= -0.51f && bz <= 0.51f && bz >= -0.51f) {
            // front face
            if (Math.abs(bz - 0.5f) <= 0.001f) {
                float imgX = 0.25f * (1f - (bx + 0.5f)) + 0.75f;
                float imgY = 0.25f * (by + 0.5f) + 0.5f;
                textureColor = textureImage.getColor(imgX, imgY);
            }
            // back face
            if (Math.abs(bz + 0.5f) <= 0.001f) {
                float imgX = 0.25f * (1f - (bx + 0.5f)) + 0.25f;
                float imgY = 0.25f * (by + 0.5f) + 0.5f;
                textureColor = textureImage.getColor(imgX, imgY);
            }
            // top face
            if (Math.abs(by - 0.5f) <= 0.001f) {
                float imgX = (bx + 0.5f) * 0.25f + 0.25f;
                float imgY = (1f - (bz + 0.5f)) * 0.25f+0.25f;
                textureColor = textureImage.getColor(imgX, imgY);
            }
            // bottom face
            if (Math.abs(by + 0.5f) <= 0.001f) {
                float imgX = (bx + 0.5f) * 0.25f + 0.25f;
                float imgY = (bz + 0.5f) * 0.25f - 0.25f;
                textureColor = textureImage.getColor(imgX, imgY);
            }

            // right face
            if (Math.abs(bx - 0.5f) <= 0.001f) {
                float imgX = (1f - (bz + 0.5f)) * 0.25f + 0.5f;
                float imgY = (by + 0.5f) * 0.25f + 0.5f;
                textureColor = textureImage.getColor(imgX, imgY);
            }
            // left face
            if (Math.abs(bx + 0.5f) <= 0.001f) {
                float imgX = (bz + 0.5f) * 0.25f;
                float imgY = (by + 0.5f) * 0.25f + 0.5f;
                textureColor = textureImage.getColor(imgX, imgY);
            }

            hitRecord.setTexture(textureColor);
        }
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


}
