import React, { useState } from 'react';

const JobSearch = () => {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [query, setQuery] = useState('');
  const [apiKey, setApiKey] = useState('');  // поле для API ключа ChatGPT
  const [gptPrompt, setGptPrompt] = useState('');  // поле для запроса в ChatGPT

  const handleSubmit = async (e) => {
    e.preventDefault();

    try {
      const response = await fetch('/api/search', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',        },
        body: JSON.stringify({
          email, 
          password, 
          query, 
          apiKey, 
          gptPrompt 
        }),
      });

      if (response.ok) {
        const data = await response.json();
        console.log('Job search and ChatGPT query successful:', data);
      } else {
        console.error('Job search or ChatGPT query failed');
      }
    } catch (error) {
      console.error('Error:', error);
    }
  };

  return (
    <form onSubmit={handleSubmit}>
      <input 
        type="email" 
        value={email} 
        onChange={(e) => setEmail(e.target.value)} 
        placeholder="Email"
      />
      <input 
        type="password" 
        value={password} 
        onChange={(e) => setPassword(e.target.value)} 
        placeholder="Password"
      />
      <input 
        type="text" 
        value={query} 
        onChange={(e) => setQuery(e.target.value)} 
        placeholder="Job Title (e.g. Java, React)"
      />
      <input 
        type="text" 
        value={apiKey} 
        onChange={(e) => setApiKey(e.target.value)} 
        placeholder="ChatGPT API Key"
      />
      <input 
        type="text" 
        value={gptPrompt} 
        onChange={(e) => setGptPrompt(e.target.value)} 
        placeholder="ChatGPT Prompt"
      />
      <button type="submit">Search & Apply</button>
    </form>
  );
};

export default JobSearch;
