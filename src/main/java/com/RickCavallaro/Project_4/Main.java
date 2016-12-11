package com.RickCavallaro.Project_4;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import com.sun.deploy.net.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.*;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.ClosableHttpClient;
import org.apache.http.HttpResponse;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;

import java.io.Closeable;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

class HttpException extends IOException {
    public HttpException(HttpResponse response) {
        super(response.getStatusLine()getStatusCode() + ": "
        +response.GetStatusLine().getReasonPhrase());
    }
}

class HttpRequests {
    private CloseableHttpClient client;

    public HttpRequests(String username, String password) {
        CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        credentialsProvider.setCredentials(AuthScope.ANY,
                new UsernamePasswordCredentials(username, password));
        client = HttpClientBuilder.create()
                .setDefaultCredentialsProvider(credentialsProvider)
                .build();
    }

    private static boolean isSuccess(HttpResponse response) {
        StatusLine statusLine = response.getStatusLine();
        return (statusLine.getStatusCode() >= 200 && statusLine.getStatusCode() < 300);
    }

    private String makeRequest(HttpRequestBase request) throws IOException {
        CloseableHttpResponse response = client.execute(request);
        try {
            if(!isSuccess(response)) {
                throw new HttpException(response);
            }
            return EntityUtils.toString(response.getEntity());
        }
        finally {
            response.close();
        }
    }

    private void addData(HttpEntityEnclosingRequestBase request, String contentType, String data) throws UnsupportedEncodingException {
        request.setHeader("Content-type", contentType);
        StringEntity requestData = new StringEntity(data);
        request.setEntity(requestData);
    }

    public String get(String url) throws IOException {
        HttpGet request = new HttpGet(url);
        return makeRequest(request);
    }

    public String delete(String url) throws IOException {
        HttpDelete request = new HttpDelete(url);
        return makeRequest(request);
    }

    public String post(String url, String contentType, String data) throws IOException {
        HttpPost request = new HttpPost(url);
        addData(request, contentType, data);
        return makeRequest(request);
    }

    public String put(String url, String contentType, String data) throws IOException {
        HttpPut request = new HttpPut(url);
        addData(request, contentType, data);
        return makeRequest(request);
    }
}

// a To-do class with a basic constructor, getters, and setters
class Todo {
    private String title;
    private String body;
    private int priority;
    private Integer id = null; //only set an int when the server assigns an ID

    public Todo(String title, String body, int priority) {
        this.title = title;
        this.body = body;
        this.priority = priority;
    }

    public String getTitle() {
        return title;

    public void setTitle(String title) {
        this.title = title;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    @Override
    public String toString() {
        return "TODO ID: " + id + ", Title: " + title + ", Body: " + body
                + ", Priority: " + priority;
    }
}

class TodoAPIWrapper {
    private Gson gson = new Gson();
    private HttpRequests requests;
    private String hostUrl;

    TodoAPIWrapper(String hostUrl, String username, String password) {
        this.hostUrl = hostUrl;
        requests = new HttpRequests(username, password);
    }

    public TodoCollection getTodos() {
        String requestUrl = hostUrl + "/todos/api/v1.0/todos";
        try {
            String response = requests.get(requestUrl);
            return gson.fromJson(response, TodoCollection.class);
        } catch (IOException e) {
            System.out.println("Could not get list of todos");
        }
        return null;
    }

    public Todo createTodo(String title, String body, int priority) {
        Todo todo = new Todo(title, body, priority);
        String requestUrl = hostUrl + "/todos/api/v1.0/todo/create";
        String contentType = "application/json";
        String postData = gson.toJson(todo);
        try {
            requests.post(requestUrl, contentType, postData);
            return todo;
        }
        catch (IOException e) {
            System.out.println("Could not create new task");
        }
        return null;
    }

    // get a todo by id
    public Todo getTodo(int id) {
        String url = hostUrl + "/todos/api/v1.0/todo/" + id;
        try {
            String response = requests.get(url);
            JsonParser parser = new JsonParser();
            JsonObject jsonObject = parser.parse(response).getAsJsonObject();
            return gson.fromJson(jsonObject.get("todo").toString(), Todo.class);
        } catch (IOException e) {
            System.out.println("Unable to get todo with id" + id);
        }
        return null;
    }


    // get first todo that matches title
    public Todo getTodo(String title) {
        TodoCollection todos = getTodos();
        for (Todo todo: todos) {
            if (todo.getTitle().equals(title)) {
                return todo;
            }
        }
        return null;
    }

    // delete a todo by id
    public boolean removeTodo(int id) {
        String url = hostUrl + "/todos/api/v1.0/todo/delete/" + id;
        try {
            requests.delete(url);
            return true;
        }
        catch (IOException e) {
            System.out.println("Unable to delete todo with ID " + id);
        }
        return false;
    }

    public boolean updateTodo(Todo newTodo) {
        if (newTodo.getId() == null) {
            return false;
        }
        String url = hostUrl + "/todos/api/v1.0/todo/update/" + newTodo.getId();
        String contentType = "application/json";
        String putData = gson.toJson(newTodo);
        try {
            requests.put(url, contentType, putData);
        } catch (IOException e) {
            System.out.println("Could not update todo");
            return false;
        }
        return true;
    }
}

/*
 * Created by Rick on 12/11/2016.
 */
public class Main {
    public static void main(String[] args) {
        TodoAPIWrapper todoAPI = new TodoAPIWrapper(
                "http://todo.eastus.cloudapp.azure.com/todo-android",
                "arthur", "arthur");
        // create two todos
        System.out.println("Adding todos");
        todoAPI.createTodo("Study", "Study for exam", 2);
        todoAPI.createTodo("Dinner", "Prepare dinner", 3);

        // get todos and list them
        System.out.println("Getting todos");
        TodoCollection todos = todoAPI.getTodos();
        for (Todo todo: todos) {
            System.out.println(todo);
        }

        // remove todo with id 0
        System.out.println("Removing todo");
        todoAPI.removeTodo(3);

        //get remaining todos and list them
        System.out.println("Getting remaining todos");
        todos = todoAPI.getTodos();
        for (Todo todo: todos) {
            System.out.println(todo);
        }

        //update existing
        Todo aTodo = todoAPI.getTodo(6);
        aTodo.setTitle("Sleep");
        aTodo.setBody("Go to bed");
        boolean updateSuccess = todoAPI.updateTodo(aTodo);
        System.out.println("Update success: " + updateSuccess );

        System.out.println("Getting updated todos");
        todos = todoAPI.getTodos();
        for (Todo todo: todos) {
            System.out.println(todo);
        }
    }
}
