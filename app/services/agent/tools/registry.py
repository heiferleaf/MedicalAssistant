"""Tool registry for dynamic tool management."""
from typing import Dict, Type, Optional, Any, Callable


class ToolRegistry:
    """Registry for managing agent tools."""
    
    def __init__(self):
        self._tools: Dict[str, Dict[str, Any]] = {}
    
    def register(self, name: str, tool_func: Callable, description: str = "", params_schema: Optional[Dict] = None):
        """Register a tool.
        
        Args:
            name: Tool name
            tool_func: Tool function
            description: Tool description
            params_schema: Parameter schema for validation
        """
        self._tools[name] = {
            "function": tool_func,
            "description": description,
            "params_schema": params_schema or {}
        }
    
    def get(self, name: str) -> Optional[Dict[str, Any]]:
        """Get a registered tool.
        
        Args:
            name: Tool name
            
        Returns:
            Tool information if found, None otherwise
        """
        return self._tools.get(name)
    
    def list(self) -> Dict[str, Dict[str, Any]]:
        """List all registered tools.
        
        Returns:
            Dict of all registered tools
        """
        return self._tools
    
    def has(self, name: str) -> bool:
        """Check if a tool is registered.
        
        Args:
            name: Tool name
            
        Returns:
            True if tool exists, False otherwise
        """
        return name in self._tools


# Global tool registry instance
registry = ToolRegistry()
