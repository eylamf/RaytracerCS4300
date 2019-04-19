package sgraph;

import com.jogamp.opengl.util.texture.Texture;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import rtHelpers.HitRecord;
import rtHelpers.Ray3D;
import util.*;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.*;
import java.util.List;

public class RaytracerRenderer implements IScenegraphRenderer {

    private Map<String, TextureImage> textures;
    private Color color;
    private List<Light> lights;
    public int width, height;
    public float fov;
    private Color refractionColor;
    private static final int MAX_REUCRRENCE_COUNT = 5;

    public RaytracerRenderer(float fov, int w, int h) {
        textures = new HashMap<>();
        this.fov = fov;
        width = w;
        height = h;
        refractionColor = new Color(0, 0, 0);
    }

    @Override
    public void setContext(Object obj) throws IllegalArgumentException {
        throw new IllegalArgumentException("WRONG RENDERER");
    }

    @Override
    public void initShaderProgram(ShaderProgram shaderProgram, Map<String, String> shaderVarsToVertexAttribs) {
        throw new IllegalArgumentException("WRONG RENDERER");
    }

    @Override
    public int getShaderLocation(String name) {
        throw new IllegalArgumentException("WRONG RENDERER");
    }

    @Override
    public <K extends IVertexData> void addMesh(String name, PolygonMesh<K> mesh) throws Exception {

    }

    @Override
    public void draw(INode root, Stack<Matrix4f> modelView) {
        Ray3D ray3D = new Ray3D();

        lights = root.getLightsInView(modelView);

        BufferedImage outputImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        Vector4f sp = new Vector4f(0, 0, 0, 1);
        ray3D.setStartPoint(sp);

        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                /*
                V = Vector(x, y, z) where:
                x = i - width / 2
                y = j - height / 2
                z = -0.5 * height / tan(0.5 * field of view)
                 */
                Vector4f dir = new Vector4f(i - 0.5f * width, j - 0.5f * height, -0.5f * height / (float) Math.tan(Math.toRadians(0.5 * fov)), 0);

                ray3D.setDirection(dir);

                HitRecord hitRecord = new HitRecord();

                color = new Color(0, 0, 0);

                raycast(ray3D, root, modelView, hitRecord);
                outputImage.setRGB(i, height - 1 - j, color.getRGB());
            }
        }

        OutputStream outputStream = null;

        try {
            outputStream = new FileOutputStream("output/raytrace.png");
        } catch (FileNotFoundException e) {
            throw new IllegalArgumentException("Failed to write to image file");
        }

        try {
            ImageIO.write(outputImage, "png", outputStream);
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to write to image file");
        }
    }

    private void raycast(Ray3D ray3D, INode root, Stack<Matrix4f> mv, HitRecord hitRecord) {
        root.intersect(ray3D, mv, hitRecord, textures);

        if (hitRecord.isHit()) {
            color = shade(hitRecord, root, mv, ray3D, 0, 1);
        } else {
            color = new Color(0, 0, 0);
        }
    }

    private Color shade(HitRecord hitRecord, INode root, Stack<Matrix4f> mv, Ray3D ray3D, int recurrenceCount, float tempRefraction) {
        Vector3f color = new Vector3f(0, 0, 0);

        for (int i = 0; i < lights.size(); i++) {
            Light light = lights.get(i);
            Vector3f lightVector;
            Vector3f spotDir = new Vector3f(light.getSpotDirection().x, light.getSpotDirection().y, light.getSpotDirection().z);

            if (spotDir.length() > 0) {
                spotDir = spotDir.normalize();
            }

            if (light.getPosition().w != 0) {
                Vector4f sp = hitRecord.getStartPoint();
                lightVector = new Vector3f(light.getPosition().x - sp.x, light.getPosition().y - sp.y, light.getPosition().z - sp.z);
            } else {
                lightVector = new Vector3f(-light.getPosition().x, -light.getPosition().y, -light.getPosition().z);
            }

            lightVector = lightVector.normalize();

            boolean isInLight = new Vector3f(lightVector).negate().dot(spotDir) <= Math.cos(Math.toRadians(light.getSpotCutoff()));
            if (isInLight) {
                continue;
            }

            Vector3f normalVector = new Vector3f(hitRecord.normal.x, hitRecord.normal.y, hitRecord.normal.z).normalize();

            float nDotL = normalVector.dot(lightVector);


            /////////////////////
            // SHADOWS FOR HW8 //
            /////////////////////

            // As per hw statement, "If it does not, ignore that light"
            boolean doesPointNotSeeLight = false;

            if (light.getPosition().w != 0) {
                Vector4f pos = new Vector4f(hitRecord.startPoint);
                Vector4f v = new Vector4f(new Vector3f(lightVector), 0);

                pos = pos.add(new Vector4f(v).mul(0.1f));

                HitRecord hit = new HitRecord();

                root.intersect(new Ray3D(pos, v), mv, hit, textures);

                if (hit.isHit()) {
                    doesPointNotSeeLight = true;
                }
            } else {
                Vector4f pos = new Vector4f(hitRecord.startPoint);
                Vector4f v = new Vector4f(new Vector3f(lightVector), 0);
                // Avoiding precision errors
                pos = pos.add(new Vector4f(v).mul(0.1f));

                HitRecord hit = new HitRecord();
                root.intersect(new Ray3D(pos, v), mv, hit, textures);

                if (hit.isHit() && light.getPosition().distance(new Vector4f(pos)) - hit.getT() > 0.0001f) {
                    doesPointNotSeeLight = true;
                }
            }

            if (doesPointNotSeeLight) {
                continue;
            }

            // Start to HW 8 - reflection

            Vector4f sp = hitRecord.startPoint;
            Vector3f viewVector = new Vector3f(sp.x, sp.y, sp.z).negate();
            viewVector = viewVector.normalize();

            Vector3f reflectionVector = new Vector3f(lightVector).negate().reflect(normalVector);
            reflectionVector = reflectionVector.normalize();

            float rDotV = Math.max(reflectionVector.dot(viewVector), 0);

            Material mat = hitRecord.getMaterial();
            Vector3f ambient = new Vector3f(mat.getAmbient().x * light.getAmbient().x, mat.getAmbient().y * light.getAmbient().y, mat.getAmbient().z * light.getAmbient().z);
            Vector3f diffuse = new Vector3f(mat.getDiffuse().x * light.getDiffuse().x * Math.max(nDotL, 0),
                    mat.getDiffuse().y * light.getDiffuse().y * Math.max(nDotL, 0),
                    mat.getDiffuse().z * light.getDiffuse().z * Math.max(nDotL, 0));

            Vector3f specular = new Vector3f(0, 0, 0);

            if (nDotL > 0) {
                specular = new Vector3f(
                  mat.getSpecular().x * light.getSpecular().x * (float) Math.pow(rDotV, mat.getShininess()),
                        mat.getSpecular().y * light.getSpecular().y * (float) Math.pow(rDotV, mat.getShininess()),
                        mat.getSpecular().z * light.getSpecular().z * (float) Math.pow(rDotV, mat.getShininess())
                );
            }

            color = new Vector3f(color).add(ambient).add(diffuse).add(specular);
        }

        // add texture
        Vector3f texture = new Vector3f(hitRecord.texture.x, hitRecord.texture.y, hitRecord.texture.z);

        color = color.mul(texture);

        float absorption = hitRecord.getMaterial().getAbsorption();

        /*
        TODO FOR HW8
        a - coeff for absorption
        r = coeff for reflection
        a + r = 1

        color at point = a * color for shading + r * color from reflection
         */
        if (absorption > 1 || absorption < 0) {
            absorption = 1;
        }

        color = color.mul(absorption);

        // TODO HW 8 REFLECTION and REFRACTION

        /*
        Refraction
        Snells law of refraction
        P = Point of intersection
        N = Noraml at P
        I = incoming ray
        T = Refracted ray
        X = Ray perp to N

        thetaI = angle between I and N
        thetaT = angle between T and -N
        MuI = refractive index of I
        MuT = refractive index of T
        sin(thetaI)/sin(thetaT) = MuT / MuI
         */

        ////////////////////////
        // REFLECTION FOR HW8 //
        ////////////////////////


        float reflection = hitRecord.material.getReflection();

        // clamp to range(0,1)
        if (reflection > 1 || reflection < 0) {
            reflection = 0;
        }

        Vector4f reflectionRef = new Vector4f(0, 0, 0, 1);

        if (reflection != 0) {
            Vector3f rayIn =
                    new Vector3f(ray3D.direction.x, ray3D.direction.y, ray3D.direction.z);

            Vector4f reflectionRay = new Vector4f(new Vector3f(rayIn).reflect
                    (hitRecord.normal.x, hitRecord.normal.y, hitRecord.normal.z), 0);

            Ray3D rayOut = new Ray3D(hitRecord.startPoint.add(new Vector4f(reflectionRay).mul(0.01f)), reflectionRay);

            raycastRecurrences(rayOut, mv, reflectionRef,
                    recurrenceCount, reflection, root, tempRefraction);
        }


        color = color.add(new Vector3f(reflectionRef.x, reflectionRef.y, reflectionRef.z));

        /////////////////////////////////////////////
        // REFRACTION EXTRA CREDIT ATTEMPT FOR HW8 //
        /////////////////////////////////////////////

        /*
        NOTE to remember: The blend is proportional to the object’s absorption,
        reflection and transparency
        */

        float transparency = hitRecord.material.getTransparency();

        Vector4f refraction = new Vector4f(0,0,0,1);

        if (transparency != 0) {
            Vector3f rayDirection = new Vector3f(ray3D.direction.x, ray3D.direction.y, ray3D
                    .direction.z);


            Vector3f normal = new Vector3f(hitRecord.normal.x, hitRecord.normal.y, hitRecord.normal.z);

            float cosIn = normal.dot(rayDirection) * -1f;
            float sinIn = 1f - ((float) Math.pow(cosIn, 2));

            // n = 1.5 for glass
            float glassIOR = 1.5f;

            float fraction;

            if (Math.abs(tempRefraction - 1f) < 0.0001f) {
                fraction = 1f / glassIOR;
            } else {
                fraction = glassIOR / 1f;
                hitRecord.normal.negate();
            }

            float sinOut = sinIn * ((float) Math.pow(fraction, 2));
            float cosOut = 1f - sinOut;

            Vector3f refractionDirection;

            if (sinOut >= 0f) {
                float cosOutR = (float) Math.sqrt(cosOut);

                refractionDirection = (new Vector3f(normal).mul(cosIn).add(rayDirection)).mul(fraction)
                        .sub(new Vector3f(normal).mul(cosOutR));

                Vector4f refractionDirectionNormalized = new Vector4f(refractionDirection, 0).normalize();

                Ray3D outRay = new Ray3D(hitRecord.startPoint.add(new Vector4f(refractionDirectionNormalized).mul(0.01f)),
                        refractionDirectionNormalized);

                HitRecord newHit = new HitRecord();

                raycastForRefraction(outRay, mv, root, newHit, recurrenceCount);

                refraction = new Vector4f((float) refractionColor.getRed()/255,
                        (float) this.refractionColor.getGreen() / 255, (float) refractionColor
                        .getBlue() / 255, 1 );
                refraction = new Vector4f(refraction.x * transparency, refraction
                        .y * transparency, refraction.z * transparency, 1);
            }

        }

        // apply refraction
        color = color.add(new Vector3f(refraction.x, refraction.y, refraction.z));

        clamp(color);

        return new Color((int) (255 * color.x), (int) (255 * color.y), (int) (255 * color.z));
    }

    private void raycastForRefraction(Ray3D outboundRay, Stack<Matrix4f> mv,  INode root, HitRecord hitRecord, int recurrenceCount) {
        if (recurrenceCount < MAX_REUCRRENCE_COUNT) {
            raycast(outboundRay, root, mv, hitRecord);
            recurrenceCount += 1;
            refractionColor = getColorForRefraction(hitRecord, root, mv, outboundRay, recurrenceCount);
        }
    }


    // clamp the color
    private void clamp(Vector3f color) {
        if (color.x < 0) {
            color.x = 0;
        } else if (color.x > 1) {
            color.x = 1;
        }

        if (color.y < 0) {
            color.y = 0;
        } else if (color.y > 1) {
            color.y = 1;
        }

        if (color.z < 0) {
            color.z = 0;
        } else if (color.z > 1) {
            color.z = 1;
        }
    }

    private void raycastRecurrences(Ray3D outboundRay, Stack<Matrix4f> mv, Vector4f tempRef, int recurrenceCount, float reflection, INode root, float tempRefraction) {
        if (recurrenceCount <= MAX_REUCRRENCE_COUNT) {

            HitRecord hitRecord = new HitRecord();
            root.intersect(outboundRay, mv, hitRecord, textures);

            boolean didHit = hitRecord.isHit();

            if (didHit) {
                recurrenceCount += 1;

                Color shadeColor = shade(hitRecord, root, mv, outboundRay, recurrenceCount, tempRefraction);
                Vector4f color = new Vector4f((float) shadeColor.getRed() / 255, (float) shadeColor.getGreen() / 255, (float) shadeColor.getBlue() / 255, 1);

                tempRef.add(color.x * reflection, color.y * reflection, color.z * reflection, 0);
            }
        }
    }

    private Color getColorForRefraction(HitRecord hitRecord, INode root, Stack<Matrix4f> mv, Ray3D ray3D, int recurrenceCount) {
        if (!hitRecord.isHit()) {
            return new Color(0,0, 0);
        }

        return shade(hitRecord, root, mv, ray3D, recurrenceCount, 1);
    }

    @Override
    public void drawMesh(String name, Material material, String textureName, Matrix4f transformation) {
        throw new IllegalArgumentException("WRONG RENDERER");
    }

    @Override
    public void addTexture(String name, String path) {
        TextureImage image = null;
        String imageFormat = path.substring(path.indexOf('.') + 1);

        try {
            image = new TextureImage(path, imageFormat, name);
        } catch (IOException e) {
            throw new IllegalArgumentException("Texture " +  path + " cannot be read!");
        }

        textures.put(name, image);
    }

    @Override
    public void initLightsInShader(List<Light> lights) {
        throw new IllegalArgumentException("WRONG RENDERER");
    }

    @Override
    public void dispose() {

    }

    @Override
    public Map<String, TextureImage> getTextures() {
        return textures;
    }
}
