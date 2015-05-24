package uk.co.maxtingle.easytv.interaction;

/**
 * A message which has content that updates, such as a progress bar
 */
public interface IPersistentMessage
{
    String updateMessage(String previousMessage);
}