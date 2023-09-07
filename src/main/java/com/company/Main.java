package com.company;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.jexl3.*;
import org.apache.commons.jexl3.introspection.JexlPermissions;
import org.apache.commons.jexl3.introspection.JexlPermissions.ClassPermissions;

@Slf4j
public class Main {


  public static void main(String[] args) {
    JexlEngine jexl;

    log.info("Welcome to JEXL 1");

    // Restricting features; no loops, no side effects
    JexlFeatures features = new JexlFeatures()
      .loops(false)
      .sideEffectGlobal(false)
      .sideEffect(false);

    // Restricted permissions to a safe set but with URI allowed
    JexlPermissions permissions = new ClassPermissions(java.net.URI.class);
    // Create the engine
    jexl = new JexlBuilder().permissions(permissions).create();

    JexlContext jexlContext = new MapContext();

    JexlScript alpha = jexl.createScript("2+2");
    Object x = alpha.execute(jexlContext);

    System.out.println(x);

    jexlContext.set("pi", 3.14);
    jexlContext.set("r", 10);

    JexlScript beta = jexl.createScript("pi*r*r");
    Object y = beta.execute(jexlContext);

    System.out.println(y);
  }
}
