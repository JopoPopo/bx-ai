/**
 * [BoxLang]
 *
 * Copyright [2023] [Ortus Solutions, Corp]
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS"
 * BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package ortus.boxlang.ai.bifs;

import static com.google.common.truth.Truth.assertThat;

import java.nio.file.Path;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.ai.BaseIntegrationTest;
import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.scopes.Key;

/**
 * Integration tests for audio BIF registration and local validation behavior.
 */
public class aiAudioTest extends BaseIntegrationTest {

	@BeforeAll
	public static void reloadModuleForAudioTests() {
		// Re-initialize runtime for this class to avoid static-final re-registration collisions on module reload
		runtime			= BoxRuntime.getInstance( true, Path.of( "src/test/resources/boxlang.json" ).toString() );
		moduleService	= runtime.getModuleService();
		loadModule( runtime.getRuntimeContext() );
	}

	@DisplayName( "Audio BIFs are registered" )
	@Test
	public void testAudioBIFRegistered() {
		// @formatter:off
		runtime.executeSource(
			"""
			audioToTextExists = getFunctionList().keyExists( "aiAudioToText" )
			textToAudioExists = getFunctionList().keyExists( "aiTextToAudio" )
			transcribeExists = getFunctionList().keyExists( "aiTranscribe" )
			speakExists = getFunctionList().keyExists( "aiSpeak" )
			""",
			context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "audioToTextExists" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "textToAudioExists" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "transcribeExists" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "speakExists" ) ) ).isTrue();
	}

	@DisplayName( "aiAudioToText() validates unsupported file extensions" )
	@Test
	public void testAudioToTextValidation() {
		// @formatter:off
		runtime.executeSource(
			"""
			errorMessage = ""
			try {
				aiAudioToText( filePath: "C:/tmp/not-supported.ogg", options: { provider: "openai" } )
			} catch( any e ) {
				errorMessage = e.message
			}
			""",
			context
		);
		// @formatter:on

		var errorMessage = variables.getAsString( Key.of( "errorMessage" ) );
		assertThat( errorMessage ).contains( "Unsupported transcription file type" );
	}

	@DisplayName( "Audio request models expose expected defaults and helpers" )
	@Test
	public void testAudioRequestModels() {
		// @formatter:off
		runtime.executeSource(
			"""
			transcribeReq = new bxModules.bxai.models.requests.AiTranscriptionRequest(
				filePath: "C:/tmp/voice.WAV",
				options: { provider: "openai" }
			)
			speechReq = new bxModules.bxai.models.requests.AiSpeechRequest(
				input: "Hello from tests",
				options: { provider: "openai" }
			)

			isTranscription = transcribeReq.isTranscription()
			isSpeech = speechReq.isSpeech()
			transcriptionExtension = transcribeReq.getFileExtension()
			transcriptionMimeType = transcribeReq.getFileMimeType()
			transcriptionReturnFormat = transcribeReq.getReturnFormat()
			speechReturnFormat = speechReq.getReturnFormat()
			""",
			context
		);
		// @formatter:on

		assertThat( variables.getAsBoolean( Key.of( "isTranscription" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "isSpeech" ) ) ).isTrue();
		assertThat( variables.getAsString( Key.of( "transcriptionExtension" ) ) ).isEqualTo( "wav" );
		assertThat( variables.getAsString( Key.of( "transcriptionMimeType" ) ) ).isEqualTo( "audio/wav" );
		assertThat( variables.getAsString( Key.of( "transcriptionReturnFormat" ) ) ).isEqualTo( "single" );
		assertThat( variables.getAsString( Key.of( "speechReturnFormat" ) ) ).isEqualTo( "binary" );
	}

	@DisplayName( "aiSpeak() validates empty input" )
	@Test
	public void testAiSpeakValidation() {
		// @formatter:off
		runtime.executeSource(
			"""
			errorMessage = ""
			try {
				aiSpeak( input: "", options: { provider: "openai" } )
			} catch( any e ) {
				errorMessage = e.message
			}
			""",
			context
		);
		// @formatter:on

		var errorMessage = variables.getAsString( Key.of( "errorMessage" ) );
		assertThat( errorMessage ).contains( "Audio speech generation requires non-empty input text" );
	}

	@DisplayName( "aiTranscribe() validates empty filePath" )
	@Test
	public void testAiTranscribeValidation() {
		// @formatter:off
		runtime.executeSource(
			"""
			errorMessage = ""
			try {
				aiTranscribe( filePath: "", options: { provider: "openai" } )
			} catch( any e ) {
				errorMessage = e.message
			}
			""",
			context
		);
		// @formatter:on

		var errorMessage = variables.getAsString( Key.of( "errorMessage" ) );
		assertThat( errorMessage ).contains( "Audio transcription requires a non-empty filePath" );
	}

	@DisplayName( "Legacy aliases route through new validation paths" )
	@Test
	public void testAudioAliases() {
		// @formatter:off
		runtime.executeSource(
			"""
			textToAudioError = ""
			audioToTextError = ""

			try {
				aiTextToAudio( input: "", options: { provider: "openai" } )
			} catch( any e ) {
				textToAudioError = e.message
			}

			try {
				aiAudioToText( filePath: "", options: { provider: "openai" } )
			} catch( any e ) {
				audioToTextError = e.message
			}
			""",
			context
		);
		// @formatter:on

		assertThat( variables.getAsString( Key.of( "textToAudioError" ) ) ).contains( "Audio speech generation requires non-empty input text" );
		assertThat( variables.getAsString( Key.of( "audioToTextError" ) ) ).contains( "Audio transcription requires a non-empty filePath" );
	}

}
