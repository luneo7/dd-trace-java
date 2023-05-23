package com.restserver;

import java.net.URISyntaxException;
import java.sql.SQLException;
import java.util.Map;
import javax.ws.rs.Consumes;
import javax.ws.rs.CookieParam;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

@Path("/hello")
public class Resource {

  @GET
  @Produces(MediaType.TEXT_PLAIN)
  public String hello() {
    return "Jersey hello world example.";
  }

  @Path("/bypathparam/{name}")
  @GET
  @Produces(MediaType.TEXT_PLAIN)
  public String byPathParam(@PathParam("name") String name) throws SQLException {
    DB.store(name);
    return "Jersey: hello " + name;
  }

  @Path("/byqueryparam")
  @GET
  @Produces(MediaType.TEXT_PLAIN)
  public String byQueryParam(@QueryParam("param") String param) throws SQLException {
    DB.store(param);
    return "Jersey: hello " + param;
  }

  @Path("/byheader")
  @GET
  @Produces(MediaType.TEXT_PLAIN)
  public String byHeader(@HeaderParam("X-Custom-header") String param) throws SQLException {
    DB.store(param);
    return "Jersey: hello " + param;
  }

  @Path("/bycookie")
  @GET
  @Produces(MediaType.TEXT_PLAIN)
  public String byCookie(@CookieParam("cookieName") String param) throws SQLException {
    DB.store(param);
    return "Jersey: hello " + param;
  }

  @Path("/puttest")
  @POST
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  public Response put(TestEntity testEntity) throws SQLException, URISyntaxException {
    DB.store(testEntity.param1);
    return Response.status(Status.CREATED).build();
  }

  @GET
  @Path("/cookiename")
  public String sourceCookieName(@Context final HttpHeaders headers) throws SQLException {
    Map<String, Cookie> cookies = headers.getCookies();
    for (Cookie cookie : cookies.values()) {
      String cookieName = cookie.getName();
      DB.store(cookieName);
      return "Jersey: hello " + cookieName;
    }
    return "cookie not found";
  }

  @GET
  @Path("/headername")
  public String sourceHeaderName(@Context final HttpHeaders headers) throws SQLException {
    for (String headerName : headers.getRequestHeaders().keySet()) {
      DB.store(headerName);
      return "Jersey: header stored";
    }
    return "cookie not found";
  }

  @GET
  @Path("/cookieobjectvalue")
  public String sourceCookieValue(@Context final HttpHeaders headers) throws SQLException {
    Map<String, Cookie> cookies = headers.getCookies();
    for (Cookie cookie : cookies.values()) {
      String cookieValue = cookie.getValue();
      DB.store(cookieValue);
      return "Jersey: hello " + cookieValue;
    }
    return "cookie not found";
  }

  @POST
  @Path("/formparameter")
  public String sourceParameterName(@FormParam("formParam1Name") final String formParam1Value)
      throws SQLException {
    DB.store(formParam1Value);
    return String.format("Jersey: hello " + formParam1Value);
  }

  @POST
  @Path("/formparametername")
  public String sourceParameterName(Form form) throws SQLException {
    for (String paramName : form.asMap().keySet()) {
      DB.store(paramName);
      return "Jersey: hello " + paramName;
    }
    return String.format("Parameter name not found");
  }
}
