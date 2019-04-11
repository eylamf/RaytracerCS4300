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
        Ray3D rayObject = new Ray3D();
        Matrix4f leafToView = new Matrix4f(mv.peek());
        Matrix4f viewToLeaf = new Matrix4f(leafToView).invert();
        rayObject.startPoint = new Vector4f(ray.startPoint);
        rayObject.direction = new Vector4f(ray.direction);

        rayObject.startPoint = viewToLeaf.transform(rayObject.startPoint);
        rayObject.direction = viewToLeaf.transform(rayObject.direction);


        if (objInstanceName.equals("sphere"))
        {
            float a,b,c;

            a = rayObject.direction.lengthSquared();
            b = 2*rayObject.startPoint.dot(rayObject.direction);
            c = rayObject.startPoint.lengthSquared()-1 - 1;

            if ((b*b-4*a*c)>=0)
            {
                float t1 = (-b+(float)Math.sqrt(b*b-4*a*c))/(2*a);
                float t2 = (-b-(float)Math.sqrt(b*b-4*a*c))/(2*a);

                float t;
                if (t1>=0)
                {
                    if (t2>=0)
                    {
                        t = Math.min(t1,t2);
                    }
                    else {
                        t = t1;
                    }
                }
                else
                {
                    if (t2>=0)
                        t = t2;
                    else
                        return;
                }

                if (t<hitRecord.time)
                {
                    hitRecord.time = t;
                    hitRecord.startPoint = new Vector4f(ray.startPoint.x+t*ray.direction.x,
                            ray.startPoint.y+t*ray.direction.y,
                            ray.startPoint.z+t*ray.direction.z,
                            1);
                    hitRecord.normal = new Vector4f(rayObject.startPoint.x+t*rayObject.direction.x,
                            rayObject.startPoint.y+t*rayObject.direction.y,
                            rayObject.startPoint.z+t*rayObject.direction.z,
                            0);

                    hitRecord.normal = new Matrix4f(viewToLeaf).transpose().transform(hitRecord.normal);
                    hitRecord.normal = new Vector4f(new Vector3f(hitRecord
                            .normal
                            .x,hitRecord.normal.y,hitRecord.normal.z)
                            .normalize(),0.0f);
                    hitRecord.material = new util.Material(this.material);


                    /**
                     * start the texture color in sphere
                     */
                    Vector4f textureC = new Vector4f(1,1,1,1);

                    TextureImage TI = textureImageMap.get(this.textureName);
                    Vector4f start = rayObject.startPoint;
                    Vector4f dir = rayObject.direction;
                    Vector4f pointInLeaf =
                            new Vector4f(start.x+dir.x*t, start.y+dir.y*t, start.z+dir.z*t, 1);
                    if (TI != null) {
                        float sphereX = pointInLeaf.x;
                        float sphereY = pointInLeaf.y;
                        float sphereZ = pointInLeaf.z;
                        float phi = (float) Math.asin(sphereY);
                        phi = phi - (float) Math.PI / 2;
                        float theta = (float) Math.atan2((double) sphereZ, (double) sphereX);
                        theta  = theta +(float) Math.PI;
                        float x = (float)(0.5-(theta / (2f * Math.PI))) ;
                        float y = (1 - (phi / ((float) Math.PI)));
                        textureC = TI.getColor(x, y);
                        hitRecord.texture = textureC;

                    }
                }

            }
        }
        else if (objInstanceName.equals("box") || objInstanceName.equals("box-outside"))
        {
            float tmaxX,tmaxY,tmaxZ;
            float tminX,tminY,tminZ;

            if (Math.abs(rayObject.direction.x)<0.0001f)
            {
                if ((rayObject.startPoint.x>0.5f) || (rayObject.startPoint.x<-0.5f))
                    return;
                else {
                    tminX = Float.NEGATIVE_INFINITY;
                    tmaxX = Float.POSITIVE_INFINITY;
                }
            }
            else
            {
                float t1 = (-0.5f-rayObject.startPoint.x)/rayObject.direction.x;
                float t2 = (0.5f-rayObject.startPoint.x)/rayObject.direction.x;
                tminX = Math.min(t1,t2);
                tmaxX = Math.max(t1,t2);
            }

            if (Math.abs(rayObject.direction.y)<0.0001f)
            {
                if ((rayObject.startPoint.y>0.5f) || (rayObject.startPoint.y<-0.5f))
                {
                    return;
                }
                else {
                    tminY = Float.NEGATIVE_INFINITY;
                    tmaxY = Float.POSITIVE_INFINITY;
                }
            }
            else
            {
                float t1 = (-0.5f-rayObject.startPoint.y)/rayObject.direction.y;
                float t2 = (0.5f-rayObject.startPoint.y)/rayObject.direction.y;
                tminY = Math.min(t1,t2);
                tmaxY = Math.max(t1,t2);
            }

            if (Math.abs(rayObject.direction.z)<0.0001f)
            {
                if ((rayObject.startPoint.z>0.5f) || (rayObject.startPoint.z<-0.5f))
                {
                    return;
                }
                else {
                    tminZ = Float.NEGATIVE_INFINITY;
                    tmaxZ = Float.POSITIVE_INFINITY;
                }
            }
            else
            {
                float t1 = (-0.5f-rayObject.startPoint.z)/rayObject.direction.z;
                float t2 = (0.5f-rayObject.startPoint.z)/rayObject.direction.z;
                tminZ = Math.min(t1,t2);
                tmaxZ = Math.max(t1,t2);
            }

            float tmin,tmax;

            tmin = Math.max(tminX,Math.max(tminY,tminZ));
            tmax = Math.min(tmaxX,Math.min(tmaxY,tmaxZ));

            if ((tmin<tmax) && (tmax>0))
            {
                float t;
                if (tmin>0)
                    t = tmin;
                else
                    t = tmax;

                if (t<hitRecord.time) {
                    hitRecord.time = t;

                    hitRecord.startPoint = new Vector4f(ray.startPoint.x + t * ray.direction.x,
                            ray.startPoint.y + t * ray.direction.y,
                            ray.startPoint.z + t * ray.direction.z,
                            1);

                    Vector4f pointInLeaf = new Vector4f(rayObject.startPoint.x + t * rayObject.direction.x,
                            rayObject.startPoint.y + t * rayObject.direction.y,
                            rayObject.startPoint.z + t * rayObject.direction.z,
                            1);

                    if (Math.abs(pointInLeaf.x - 0.5f) < 0.001)
                        hitRecord.normal.x = 1;
                    else if (Math.abs(pointInLeaf.x + 0.5f) < 0.001)
                        hitRecord.normal.x = -1;
                    else
                        hitRecord.normal.x = 0;

                    if (Math.abs(pointInLeaf.y - 0.5f) < 0.001)
                        hitRecord.normal.y = 1;
                    else if (Math.abs(pointInLeaf.y + 0.5f) < 0.001)
                        hitRecord.normal.y = -1;
                    else
                        hitRecord.normal.y = 0;

                    if (Math.abs(pointInLeaf.z - 0.5f) < 0.001)
                        hitRecord.normal.z = 1;
                    else if (Math.abs(pointInLeaf.z + 0.5f) < 0.001)
                        hitRecord.normal.z = -1;
                    else
                        hitRecord.normal.z = 0;

                    hitRecord.normal.w = 0;
                    hitRecord.normal.normalize();


                    hitRecord.normal = new Matrix4f(viewToLeaf).transpose().transform(hitRecord.normal);
                    hitRecord.normal = new Vector4f(new Vector3f(hitRecord
                            .normal
                            .x,hitRecord.normal.y,hitRecord.normal.z)
                            .normalize(),0.0f);
                    hitRecord.material = new util.Material(this.material);

                    /**
                     * start the texture color
                     */
                    Vector4f textureC = new Vector4f(1,1,1,1);

                    TextureImage TI = textureImageMap.get(this.textureName);
                    if (TI != null) {
                        float boxX = pointInLeaf.x;
                        float boxY = pointInLeaf.y;
                        float boxZ = pointInLeaf.z;

                        if (boxX<=0.51f && boxX>=-0.51f&& boxY<=0.51f && boxY>=-0.51f
                                &&boxZ <=0.51f&&boxZ>=-0.51f) {
                            // front: boxZ == 0.5
                            if (Math.abs(boxZ - 0.5f) <= 0.001f) {
                                // 4
                                float imgX = 0.25f*(1f - (boxX + 0.5f)) + 0.75f;
                                float imgY = 0.25f* (boxY + 0.5f)+0.5f;
                                textureC = TI.getColor(imgX, imgY);
                            }
                            // back: boxZ == -0.5
                            if (Math.abs(boxZ + 0.5f) <= 0.001f) {
                                //2
                                float imgX = 0.25f*(1f - (boxX + 0.5f)) + 0.25f;
                                float imgY = 0.25f* (boxY + 0.5f)+0.5f;
                                textureC = TI.getColor(imgX, imgY);
                            }
                            // right: x == 0.5
                            if (Math.abs(boxX - 0.5f) <= 0.001f) {
                                //3
                                float imgX = (1f - (boxZ + 0.5f))*0.25f+0.5f;
                                float imgY = (boxY + 0.5f)*0.25f+0.5f;
                                textureC = TI.getColor(imgX, imgY);
                            }
                            // left: x == -0.5
                            if (Math.abs(boxX + 0.5f) <= 0.001f) {
                                // 1
                                float imgX = (boxZ + 0.5f)*0.25f;
                                float imgY = (boxY + 0.5f)*0.25f+0.5f;
                                textureC = TI.getColor(imgX, imgY);
                            }
                            // top: y == 0.5
                            if (Math.abs(boxY - 0.5f) <= 0.001f) {
                                //5
                                float imgX = (boxX + 0.5f)*0.25f + 0.25f;
                                float imgY = (1f - (boxZ + 0.5f))*0.25f+0.25f;
                                textureC = TI.getColor(imgX, imgY);
                            }
                            // bottom: y == -0.5
                            if (Math.abs(boxY + 0.5f) <= 0.001f) {
                                //6
                                float imgX = (boxX + 0.5f)*0.25f+0.25f;
                                float imgY = (boxZ + 0.5f)*0.25f - 0.25f;
                                textureC = TI.getColor(imgX, imgY);
                            }
                            hitRecord.texture = textureC;
                        }
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
