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
        Ray3D newRay = new Ray3D(ray);
        Matrix4f leaf = new Matrix4f(mv.peek());
        Matrix4f view = new Matrix4f(leaf).invert();

        newRay.setStartPoint(view.transform(newRay.getStartPoint()));
        newRay.setDirection(view.transform(newRay.getDirection()));

        if (objInstanceName.equals("box") || objInstanceName.equals("box-outside")) {
            float txMax, tyMax, tzMax;
            float txMin, tyMin, tzMin;

            // Check again 0.0001f to avoid black specs on objects
            if (Math.abs(newRay.getDirection().x) < 0.0001f) {
                if (newRay.getStartPoint().x > 0.5f || newRay.getStartPoint().x < -0.5f) {
                    return;
                } else {
                    txMin = -1 * Float.MAX_VALUE;
                    txMax = Float.MAX_VALUE;
                }
            } else {
                float t = (-0.5f - newRay.getStartPoint().x) / newRay.getDirection().x;
                float t2 = (0.5f - newRay.getStartPoint().x) / newRay.getDirection().x;

                txMin = Math.min(t, t2);
                txMax = Math.max(t, t2);
            }

            if (Math.abs(newRay.getDirection().y) < 0.0001f) {
                if (newRay.getStartPoint().y > 0.5f || newRay.getStartPoint().y < -0.5f) {
                    return;
                } else {
                    tyMin = -1 * Float.MAX_VALUE;
                    tyMax = Float.MAX_VALUE;
                }
            } else {
                float t = (-0.5f - newRay.getStartPoint().y) / newRay.getDirection().y;
                float t2 = (0.5f - newRay.getStartPoint().y) / newRay.getDirection().y;

                tyMin = Math.min(t, t2);
                tyMax = Math.max(t, t2);
            }

            if (Math.abs(newRay.getDirection().z) < 0.0001f) {
                if (newRay.getStartPoint().z > 0.5f || newRay.getStartPoint().z < -0.5f) {
                    return;
                } else {
                    tzMin = -1 * Float.MAX_VALUE;
                    tzMax = Float.MAX_VALUE;
                }
            } else {
                float t = (-0.5f - newRay.getStartPoint().z) / newRay.getDirection().z;
                float t2 = (0.5f - newRay.getStartPoint().z) / newRay.getDirection().z;

                tzMin = Math.min(t, t2);
                tzMax = Math.max(t, t2);
            }

            float minT, maxT;

            minT = Math.max(txMin, Math.max(tyMin, tzMin));
            maxT = Math.min(txMax, Math.max(tyMax, tzMax));

            if (minT < maxT && maxT > 0) {
                float t = maxT;

                if (minT > 0) {
                    t = minT;
                }

                if (t < hitRecord.getT()) {
                    hitRecord.setT(t);

                    Vector4f point = new Vector4f(ray.getStartPoint().x + t * ray.getDirection().x,
                            ray.getStartPoint().y + t * ray.getDirection().y,
                            ray.getStartPoint().z + t * ray.getDirection().z, 1);
                    hitRecord.setStartPoint(point);

                    Vector4f pointAtLeaf = new Vector4f(newRay.getStartPoint().x + t * newRay.getDirection().x,
                            newRay.getStartPoint().y + t * newRay.getDirection().y,
                            newRay.getStartPoint().z + t * newRay.getDirection().z, 1);

                    Vector4f normal = new Vector4f(hitRecord.getNormal());

                    // x
                    if (Math.abs(pointAtLeaf.x - 0.5f) < 0.001f) {
                        normal.x = 1;
                    } else if (Math.abs(pointAtLeaf.x + 0.5f) < 0.001f) {
                        normal.x = -1;
                    } else {
                        normal.x = 0;
                    }
                    // y
                    if (Math.abs(pointAtLeaf.y - 0.5f) < 0.001f) {
                        normal.y = 1;
                    } else if (Math.abs(pointAtLeaf.y + 0.5f) < 0.001f) {
                        normal.y = -1;
                    } else {
                        normal.y = 0;
                    }
                    // z
                    if (Math.abs(pointAtLeaf.z - 0.5f) < 0.001f) {
                        normal.z = 1;
                    } else if (Math.abs(pointAtLeaf.z + 0.5f) < 0.001f) {
                        normal.z = -1;
                    } else {
                        normal.z = 0;
                    }

                    normal.w = 0;
                    normal = normal.normalize();

                    normal = new Matrix4f(view).transpose().transform(normal);
                    normal = new Vector4f(new Vector3f(normal.x, normal.y, normal.z).normalize(), 0);

                    hitRecord.setNormal(normal);

                    hitRecord.setMaterial(new Material(material));

                    // Color for texture
                    Vector4f textureColor = new Vector4f(1, 1, 1, 1);

                    TextureImage textureImage = textureImageMap.get(textureName);

                    // Map textureImage coords correctly
                    if (textureImage != null) {
                        float boxX = pointAtLeaf.x;
                        float boxY = pointAtLeaf.y;
                        float boxZ = pointAtLeaf.z;

                        if (boxX <= 0.51f && boxX >= -0.51f && boxY <= 0.51f && boxY >= -0.5f && boxZ <= 0.51f && boxZ >= -0.51f) {
                            // front
                            if (Math.abs(boxZ - 0.5f) <= 0.001f) {
                                float imgX = 0.25f * (1f - (boxX + 0.5f)) + 0.75f;
                                float imgY = 0.25f * (boxY + 0.5f) + 0.5f;
                                textureColor = textureImage.getColor(imgX, imgY);
                            }

                            // back
                            if (Math.abs(boxZ + 0.5f) <= 0.001f) {
                                float imgX = 0.25f * (1f - (boxX + 0.5f)) + 0.25f;
                                float imgY = 0.25f * (boxY + 0.5f) + 0.5f;
                                textureColor = textureImage.getColor(imgX, imgY);
                            }

                            // right
                            if (Math.abs(boxX - 0.5f) <= 0.001f) {
                                float imgX = (1f - (boxZ + 0.5f)) * 0.25f + 0.5f;
                                float imgY = (boxY + 0.5f) * 0.25f + 0.5f;
                                textureColor = textureImage.getColor(imgX, imgY);
                            }
                            // left
                            if (Math.abs(boxX + 0.5f) <= 0.001f) {
                                // 1
                                float imgX = (boxZ + 0.5f)*0.25f;
                                float imgY = (boxY + 0.5f)*0.25f+0.5f;
                                textureColor = textureImage.getColor(imgX, imgY);
                            }
                            // top: y == 0.5
                            if (Math.abs(boxY - 0.5f) <= 0.001f) {
                                //5
                                float imgX = (boxX + 0.5f)*0.25f + 0.25f;
                                float imgY = (1f - (boxZ + 0.5f))*0.25f+0.25f;
                                textureColor = textureImage.getColor(imgX, imgY);
                            }
                            // bottom: y == -0.5
                            if (Math.abs(boxY + 0.5f) <= 0.001f) {
                                //6
                                float imgX = (boxX + 0.5f)*0.25f+0.25f;
                                float imgY = (boxZ + 0.5f)*0.25f - 0.25f;
                                textureColor = textureImage.getColor(imgX, imgY);
                            }

                            hitRecord.setTexture(textureColor);
                        }
                    }

                }
            }
        } else if (objInstanceName.equals("sphere")) {
            float a = newRay.getDirection().lengthSquared();
            float b = 2 * newRay.getStartPoint().dot(newRay.getDirection());
            float c = newRay.getStartPoint().lengthSquared() - 2;

            // Quadratic
            if ((Math.pow(b, 2) - 4 * a * c) >= 0) {
                float t = (-b + (float) Math.sqrt(Math.pow(b, 2) - 4 * a * c)) / (2 * a);
                float t2 = (-b - (float) Math.sqrt(Math.pow(b, 2) - 4 * a * c)) / (2 * a);

                float finalT;

                if (t >= 0) {
                    if (t2 >= 0) {
                        finalT = Math.min(t, t2);
                    } else {
                        finalT = t;
                    }
                } else {
                    if (t2 >= 0) {
                        finalT = t2;
                    } else {
                        return;
                    }
                }

                if (finalT < hitRecord.getT()) {
                    hitRecord.setT(finalT);

                    Vector4f point = new Vector4f(ray.getStartPoint().x + finalT * ray.getDirection().x,
                            ray.getStartPoint().y + finalT * ray.getDirection().y,
                            ray.getStartPoint().z + finalT * ray.getDirection().z, 1);
                    hitRecord.setStartPoint(point);

                    Vector4f normal = new Vector4f(
                      newRay.getStartPoint().x + finalT * newRay.getDirection().x,
                            newRay.getStartPoint().y + finalT * newRay.getDirection().y,
                            newRay.getStartPoint().z + finalT * newRay.getDirection().z, 0);

                    normal = new Matrix4f(view).transpose().transform(normal);
                    normal = new Vector4f(new Vector3f(normal.x, normal.y, normal.z).normalize(), 0);

                    hitRecord.setNormal(normal);

                    hitRecord.setMaterial(new Material(material));

                    // Texture color
                    Vector4f textureColor = new Vector4f(1, 1, 1, 1);
                    TextureImage textureImage = textureImageMap.get(textureName);
                    Vector4f sp = newRay.getStartPoint();
                    Vector4f dir = newRay.getDirection();
                    Vector4f pointAtLeaf = new Vector4f(sp.x + dir.x * finalT, sp.y + dir.y * finalT, sp.z + dir.z * finalT, 1);

                    if (textureImage != null) {
                        float sphereX = pointAtLeaf.x;
                        float sphereY = pointAtLeaf.y;
                        float sphereZ = pointAtLeaf.z;
                        float phi = (float) Math.asin(sphereY);
                        phi = phi - (float) Math.PI / 2;
                        float theta = (float) Math.atan2((double) sphereZ, (double) sphereX);
                        theta = theta + (float) Math.PI;
                        float x = (float) (0.5 - (theta / (2 * Math.PI)));
                        float y = (1 - (phi / ((float) Math.PI)));
                        textureColor = textureImage.getColor(x, y);
                        hitRecord.setTexture(textureColor);
                    }
                }
            }
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
