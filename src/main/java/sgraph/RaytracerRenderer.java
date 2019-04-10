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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

public class RaytracerRenderer implements IScenegraphRenderer {

    private Map<String, TextureImage> textures;
    private Color color;
    private List<Light> lights;
    public int width, height;
    public float fov;

    public RaytracerRenderer(float fov, int w, int h) {
        textures = new HashMap<>();
        this.fov = fov;
        width = w;
        height = h;
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
            color = shade(hitRecord, root, mv, ray3D, 0);
        } else {
            color = new Color(0, 0, 0);
        }
    }

    private Color shade(HitRecord hitRecord, INode root, Stack<Matrix4f> mv, Ray3D ray3D, int count) {
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

            Vector4f normal = hitRecord.getNormal();
            Vector3f normalVector = new Vector3f(normal.x, normal.y, normal.z).normalize();

            float nDotL = normalVector.dot(lightVector);

            boolean notSee = false;

            if (light.getPosition().w != 0) {
                Vector4f p = new Vector4f(hitRecord.getStartPoint());
                Vector4f v = new Vector4f(new Vector3f(lightVector), 0);

                p = p.add(new Vector4f(v).mul(0.1f));

                HitRecord hit = new HitRecord();

                root.intersect(new Ray3D(p, v), mv, hit, textures);

                if (hit.isHit()) {
                    notSee = true;
                }
            } else {
                Vector4f p = new Vector4f(hitRecord.getStartPoint());
                Vector4f v = new Vector4f(new Vector3f(lightVector), 0);
                p = p.add(new Vector4f(v).mul(0.1f));

                HitRecord hit = new HitRecord();
                root.intersect(new Ray3D(p, v), mv, hit, textures);

                if (hit.isHit() && light.getPosition().distance(new Vector4f(p)) - hit.getT() > 0.0001f) {
                    notSee = true;
                }
            }

            if (notSee) {
                continue;
            }

            // Start to HW 8 - reflection
            Vector4f sp = hitRecord.getStartPoint();
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
        Vector3f texture = new Vector3f(hitRecord.getTexture().x, hitRecord.getTexture().y, hitRecord.getTexture().z);

        color = color.mul(texture);

        float absorption = hitRecord.getMaterial().getAbsorption();

        if (absorption > 1 || absorption < 0) {
            absorption = 1;
        }

        color = color.mul(absorption);

        // TODO HW 8 REFLECTION and REFRACTION

        clamp(color);

        return new Color((int) (255 * color.x), (int) (255 * color.y), (int) (255 * color.z));
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
//        throw new IllegalArgumentException("WRONG RENDERER");
    }

    @Override
    public void dispose() {

    }

    @Override
    public Map<String, TextureImage> getTextures() {
        return textures;
    }
}
