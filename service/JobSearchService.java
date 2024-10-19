package com.example.demo.service;

import java.util.Random;

import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

@Service
public class JobSearchService {

    // Метод для запроса к ChatGPT
    // Метод для запроса к ChatGPT, включающий переданный gptPrompt
    public String askChatGPT(String apiKey, String userPrompt, Object gptPrompt) throws Exception {
        URL url = new URL("https://api.openai.com/v1/chat/completions");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Authorization", "Bearer " + apiKey);
        conn.setRequestProperty("Content-Type", "application/json");

        // Формируем сообщение для chat-based модели
        JsonObject data = new JsonObject();
        data.addProperty("model", "gpt-3.5-turbo");

        // Массив сообщений, где указываем роль пользователя, gptPrompt и текст его вопроса
        JsonObject message = new JsonObject();
        message.addProperty("role", "user");
        message.addProperty("content", "Ответь кратко и без комментариев: " + userPrompt + ". Резюме: " + gptPrompt);

        // Собираем объект запроса
        data.add("messages", new com.google.gson.JsonArray());
        data.getAsJsonArray("messages").add(message);

        conn.setDoOutput(true);
        OutputStream os = conn.getOutputStream();
        os.write(data.toString().getBytes());
        os.flush();
        os.close();

        int responseCode = conn.getResponseCode();

        if (responseCode == 200) {
            // Успешный ответ
            Scanner scanner = new Scanner(conn.getInputStream());
            StringBuilder response = new StringBuilder();
            while (scanner.hasNext()) {
                response.append(scanner.nextLine());
            }
            scanner.close();

            JsonObject responseJson = JsonParser.parseString(response.toString()).getAsJsonObject();
            String gptAnswer = responseJson.getAsJsonArray("choices").get(0).getAsJsonObject().get("message").getAsJsonObject().get("content").getAsString().trim();

            // Печатаем ответ ChatGPT для отладки
            System.out.println("Ответ ChatGPT: " + gptAnswer);

            return gptAnswer;
        } else {
            // Ошибка
            InputStream errorStream = conn.getErrorStream();
            if (errorStream != null) {
                Scanner scanner = new Scanner(errorStream);
                StringBuilder errorResponse = new StringBuilder();
                while (scanner.hasNext()) {
                    errorResponse.append(scanner.nextLine());
                }
                scanner.close();
                JsonObject errorJson = JsonParser.parseString(errorResponse.toString()).getAsJsonObject();
                String errorMessage = errorJson.has("error") ? errorJson.getAsJsonObject("error").get("message").getAsString() : "Unknown error";

                throw new RuntimeException("Ошибка при запросе к ChatGPT: HTTP " + responseCode + ". Сообщение: " + errorMessage);
            } else {
                throw new RuntimeException("Ошибка при запросе к ChatGPT: HTTP " + responseCode + ". Подробное сообщение об ошибке отсутствует.");
            }
        }
    }

    // Метод для поиска и отклика на вакансии
    // Метод для поиска и отклика на вакансии
    public void searchAndApply(String email, String password, String query, String apiKey, Object gptPrompt) throws InterruptedException {
        System.setProperty("webdriver.chrome.driver", "/opt/homebrew/bin/chromedriver");
        ChromeOptions options = new ChromeOptions();

        // Add stealth settings to avoid detection
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--disable-blink-features=AutomationControlled");

        // Set experimental options to prevent WebDriver detection
        options.setExperimentalOption("excludeSwitches", Arrays.asList("enable-automation"));
        options.setExperimentalOption("useAutomationExtension", false);

        // Optionally, change user-agent string to make it appear like a normal browser
        options.addArguments("user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36");


        WebDriver driver = new ChromeDriver(options);

        try {
            driver.get("https://hh.ru/account/login");
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(60));

            // Логинимся
            WebElement emailInput = wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("input[name='login']")));
            emailInput.sendKeys(email);

            WebElement passwordLink = wait.until(ExpectedConditions.elementToBeClickable(By.linkText("Войти с паролем")));
            passwordLink.click();

            WebElement passwordInput = wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("input[type='password']")));
            passwordInput.sendKeys(password);

            WebElement loginButton = wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector("button[data-qa='account-login-submit']")));
            loginButton.click();

            System.out.println("Please solve the captcha manually.");
            waitForCaptchaToBeSolved(driver);

            Thread.sleep(5000);

            // Ищем вакансии
            driver.get("https://hh.ru/");
            WebElement searchInput = wait.until(ExpectedConditions.visibilityOfElementLocated(By.name("text")));
            searchInput.sendKeys(query);
            WebElement searchButton = wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector("button[data-qa='search-button']")));
            searchButton.click();

            // Закрыть баннер cookies
            try {
                WebElement cookieButton = driver.findElement(By.xpath("//*[contains(text(), 'Понятно')]"));
                if (cookieButton.isDisplayed()) {
                    cookieButton.click();
                    System.out.println("Cookie banner closed.");
                }
            } catch (NoSuchElementException e) {
                System.out.println("No cookie banner found.");
            }

            boolean foundAnyJobs = false;

            while (true) {
                randomDelay();
                try {
                    WebElement applyButton = null;

                    while (applyButton == null) {
                        try {
                            // Ищем кнопку "Откликнуться"
                            applyButton = driver.findElement(By.xpath("//*[contains(text(), 'Откликнуться')]"));

                            if (applyButton != null) {
                                foundAnyJobs = true; // Установить флаг, если найдена хотя бы одна вакансия
                            }

                            // Прокручиваем страницу до кнопки отклика
                            slowlyScrollIntoView(driver, applyButton);
                            wait.until(ExpectedConditions.elementToBeClickable(applyButton)).click();

                            // Проверяем наличие радиокнопок
                            List<WebElement> radioButtons = driver.findElements(By.cssSelector("input[type='radio']"));
                            if (!radioButtons.isEmpty()) {
                                System.out.println("Радиокнопки найдены, выбираем первый вариант.");
                                WebElement firstRadioButton = radioButtons.get(0); // выбираем первую радиокнопку

                                // Вызываем наш метод для выбора первой радиокнопки
                                selectFirstRadioButton(driver, wait, firstRadioButton);
                                continue; // Переход к следующей итерации
                            }

                            // Проверяем наличие формы для заполнения
                            List<WebElement> textareas = driver.findElements(By.cssSelector("[data-qa='task-body'] textarea"));
                            if (!textareas.isEmpty()) {
                                for (WebElement textarea : textareas) {
                                    WebElement questionElement = textarea.findElement(By.xpath("preceding::div[@data-qa='task-question'][1]"));
                                    String question = questionElement.getText();

                                    // Получаем ответ от ChatGPT
                                    String gptAnswer = askChatGPT(apiKey, question, gptPrompt);
                                    textarea.sendKeys(gptAnswer);
                                }

                                // Нажимаем на кнопку "Откликнуться"
                                WebElement respondButton = driver.findElement(By.cssSelector("[data-qa='vacancy-response-button-top']"));
                                respondButton.click();
                                System.out.println("Ответы заполнены и отправлены.");
                            } else {
                                // Если текстовых полей нет, просто нажимаем на кнопку "Откликнуться"
                                if (applyButton != null) {
                                    wait.until(ExpectedConditions.elementToBeClickable(applyButton)).click();
                                    System.out.println("Простой отклик: нажата кнопка 'Откликнуться'.");
                                }
                            }

                        } catch (StaleElementReferenceException e) {
                            System.out.println("Элемент устарел, пытаемся найти его снова...");
                        } catch (NoSuchElementException e) {
                            System.out.println("Кнопка 'Откликнуться' не найдена, продолжаем прокрутку...");
                            slowlyScrollDown(driver, 10);  // Медленная прокрутка вниз
                        }
                    }

                } catch (Exception e) {
                    System.out.println("Произошла ошибка: " + e.getMessage());
                }

                if (!foundAnyJobs) {
                    System.out.println("Никакие вакансии не найдены. Завершаем процесс.");
                    break;
                }

                slowlyScrollDown(driver, 1000);
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            driver.quit();
        }
    }

    // Метод для медленной прокрутки страницы вниз
    private void slowlyScrollDown(WebDriver driver, int pixels) throws InterruptedException {
        JavascriptExecutor js = (JavascriptExecutor) driver;
        for (int i = 0; i < pixels; i += 500) {
            js.executeScript("window.scrollBy(0, 50);");
            Thread.sleep(1000);
        }
    }

    // Метод для медленной прокрутки к элементу
    private void slowlyScrollIntoView(WebDriver driver, WebElement element) throws InterruptedException {
        JavascriptExecutor js = (JavascriptExecutor) driver;
        for (int i = 0; i < 10; i++) {
            js.executeScript("arguments[0].scrollIntoView({block: 'center', inline: 'nearest'});", element);
            Thread.sleep(1000);
        }
    }

    private void waitForCaptchaToBeSolved(WebDriver driver) {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofMinutes(5));
        wait.until(ExpectedConditions.invisibilityOfElementLocated(By.className("captcha-class-name")));
    }

    public void selectFirstRadioButton(WebDriver driver, WebDriverWait wait, WebElement radioButton) {
        try {
            System.out.println("try MODERN tech");
            if (!radioButton.isDisplayed()) {
                System.out.println("Элемент не виден, начинаем скроллирование...");
                JavascriptExecutor js = (JavascriptExecutor) driver;

                // Медленный скроллинг к элементу
                for (int i = 0; i < 20; i++) { // Попробуем несколько шагов скроллинга
                    js.executeScript("window.scrollBy(0, 10);"); // Скроллим на 200px за шаг
                    Thread.sleep(1000); // Даем странице отрисоваться

                    if (radioButton.isDisplayed()) {
                        System.out.println("Элемент стал видимым, останавливаем скроллирование.");
                        break;
                    }
                }
            }
            Thread.sleep(1000); // Даем странице отрисоваться
            randomDelay();

            // Ждем, пока радиокнопка станет кликабельной
            wait.until(ExpectedConditions.elementToBeClickable(radioButton));

            // Пробуем кликнуть напрямую
            try {
                radioButton.click();
                System.out.println("Радиокнопка выбрана обычным кликом.");
            } catch (ElementClickInterceptedException e) {
                // Если клик перехвачен, используем JavaScript
                System.out.println("Обычный клик не удался. Используем JavaScript для клика.");
                JavascriptExecutor js = (JavascriptExecutor) driver;
                js.executeScript("arguments[0].click();", radioButton);
            }

        } catch (ElementNotInteractableException e) {
            System.out.println("Элемент недоступен для взаимодействия: " + e.getMessage());
        } catch (StaleElementReferenceException e) {
            System.out.println("Элемент стал неактуальным: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.out.println("Операция прервана: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("Произошла ошибка при выборе радиокнопки: " + e.getMessage());
        }


    }
    public static void randomDelay() {
        Random random = new Random();

        // Predefined range for delay: 1000ms (1 second) to 5000ms (5 seconds)
        int minDelay = 1;
        int maxDelay = 50;

        // Generate a random delay within the range [minDelay, maxDelay]
        int randomDelay = minDelay + random.nextInt(maxDelay - minDelay);

        try {
            System.out.println("Sleeping for " + randomDelay + " milliseconds.");

            // Pause execution for the generated random delay
            Thread.sleep(randomDelay);

            System.out.println("Resumed after " + randomDelay + " milliseconds delay.");
        } catch (InterruptedException e) {
            System.out.println("Interrupted during sleep: " + e.getMessage());
        }
    }
}