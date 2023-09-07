package com.company;


import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.jexl3.*;
import org.apache.commons.jexl3.introspection.JexlPermissions;
import org.apache.commons.jexl3.introspection.JexlPermissions.ClassPermissions;
import org.junit.Assert;
import org.junit.Test;

/**
 * A test around scripting streams.
 */
public class StreamTest {

  /**
   * Our engine instance.
   */
  private final JexlEngine jexl;

  public StreamTest() {
    // Restricting features; no loops, no side effects
    JexlFeatures features = new JexlFeatures()
      .loops(false)
      .sideEffectGlobal(false)
      .sideEffect(false);
    // Restricted permissions to a safe set but with URI allowed
    JexlPermissions permissions = new ClassPermissions(java.net.URI.class);
    // Create the engine
    jexl = new JexlBuilder().permissions(permissions).create();
  }

  /**
   * A MapContext that can operate on streams.
   */
  public static class StreamContext extends MapContext {

    /**
     * This allows using a JEXL lambda as a mapper.
     *
     * @param stream the stream
     * @param mapper the lambda to use as mapper
     * @return the mapped stream
     */
    public Stream<?> map(Stream<?> stream, final JexlScript mapper) {
      return stream.map(x -> mapper.execute(this, x));
    }

    /**
     * This allows using a JEXL lambda as a filter.
     *
     * @param stream the stream
     * @param filter the lambda to use as filter
     * @return the filtered stream
     */
    public Stream<?> filter(Stream<?> stream, final JexlScript filter) {
      //return stream.filter(x -> x =! null "" TRUE.equals(filter.execute(this, x)));
      return null;
    }
  }

  @Test
  public void testURIStream() throws Exception {
    // let's assume a collection of uris need to be processed and transformed to be simplified ;
    // we want only http/https ones, only the host part and forcing an https scheme
    List<URI> uris = Arrays.asList(
      URI.create("http://user@www.apache.org:8000?qry=true"),
      URI.create("https://commons.apache.org/releases/prepare.html"),
      URI.create("mailto:henrib@apache.org")
    );
    // Create the test control, the expected result of our script evaluation
    List<?> control = uris.stream()
      .map(uri -> uri.getScheme().startsWith("http") ? "https://" + uri.getHost() : null)
      .filter(x -> x != null)
      .collect(Collectors.toList());
    Assert.assertEquals(2, control.size());

    // Create scripts:
    // uri is the name of the variable used as parameter; the beans are exposed as properties
    // note the starts-with operator =^
    // note that uri is also used in the back-quoted string that performs variable interpolation
    JexlScript mapper = jexl.createScript("uri.scheme =^ 'http'? `https://${uri.host}` : null",
      "uri");
    // using the bang-bang / !! - JScript like -  is the way to coerce to boolean in the filter
    JexlScript transform = jexl.createScript(
      "list.stream().map(mapper).filter(x -> !!x).collect(Collectors.toList())", "list");

    // Execute scripts:
    JexlContext sctxt = new StreamContext();
    // expose the static methods of Collectors; java.util.* is allowed by permissions
    sctxt.set("Collectors", Collectors.class);
    // expose the mapper script as a global variable in the context
    sctxt.set("mapper", mapper);

    Object transformed = transform.execute(sctxt, uris);
    Assert.assertTrue(transformed instanceof List<?>);
    Assert.assertEquals(control, transformed);
  }
}
